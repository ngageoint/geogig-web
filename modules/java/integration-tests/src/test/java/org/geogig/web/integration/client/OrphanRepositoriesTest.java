package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.web.client.Repo;
import org.geogig.web.client.ReposClient;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.FileStoreInfo;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RepositoryInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.plumbing.remotes.RemoteAddOp;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PullResult;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;

import com.google.common.base.Optional;

public class OrphanRepositoriesTest extends AbstractIntegrationTest {

    private Store store;

    private User user1, user2;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user1 = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);
        user2 = testSupport.createUser("dave", "s3cr3t", "David Blasby", "dave@example.com",
                storeName);

        repo = user1.createRepo("naturalEearth");
    }

    public @Test void getOrphansByStoreWithoutPreviouslyConfiguredUUID() {
        final Repository orphan1;
        final Repository orphan2;

        orphan1 = createOrphan("volaya:orphan1");
        orphan2 = clone(orphan1, "dblasby:orphan2");
        assertNotNull(orphan2);

        List<RepositoryInfo> orphanRepos = store.getOrphanRepos();
        assertNotNull(orphanRepos);
        assertEquals(2, orphanRepos.size());
        Map<String, RepositoryInfo> byName = orphanRepos.stream()
                .collect(Collectors.toMap(r -> r.getIdentity(), r -> r));

        RepositoryInfo volayaRepo = byName.get("volaya:orphan1");
        RepositoryInfo dblasbyRepo = byName.get("dblasby:orphan2");
        assertNotNull(volayaRepo);
        assertNotNull(dblasbyRepo);
        assertNotNull(volayaRepo.getId());
        assertNotNull(dblasbyRepo.getId());
        // the ids must have been assigned and so the forked from id based on the origin URI
        RepositoryInfo forkedFrom = dblasbyRepo.getForkedFrom();
        assertNotNull(forkedFrom);
        assertEquals(volayaRepo, forkedFrom);
    }

    public @Test void getOrphansByStoreWithPreviouslyConfiguredUUID() {
        UUID orphanId1 = UUID.randomUUID();
        UUID orphanId2 = UUID.randomUUID();

        Repository orphan1 = createOrphan("orphan1", orphanId1, null);
        Repository orphan2 = createOrphan("orphan2", orphanId2, orphanId1);
        assertNotNull(orphan1);
        assertNotNull(orphan2);
        List<RepositoryInfo> orphanRepos = store.getOrphanRepos();
        assertNotNull(orphanRepos);
        assertEquals(2, orphanRepos.size());
        Map<String, RepositoryInfo> byName = orphanRepos.stream()
                .collect(Collectors.toMap(r -> r.getIdentity(), r -> r));

        RepositoryInfo orphan1Info = byName.get("orphan1");
        RepositoryInfo orphan2Info = byName.get("orphan2");
        assertNotNull(orphan1Info);
        assertNotNull(orphan2Info);
        assertEquals(orphanId1, orphan1Info.getId());
        assertEquals(orphanId2, orphan2Info.getId());

    }

    public @Test void transferOrphanRepositoryToExistingUser() {
        Repository orphan1 = createOrphan("volaya:orphan1");
        assertNotNull(orphan1);
        Repository orphan2 = clone(orphan1, "dblasby:orphan2");
        assertNotNull(orphan2);

        Map<String, RepositoryInfo> byName = store.getOrphanRepos().stream()
                .collect(Collectors.toMap(r -> r.getIdentity(), r -> r));
        RepositoryInfo volayaRepo = byName.get("volaya:orphan1");
        RepositoryInfo dblasbyRepo = byName.get("dblasby:orphan2");
        assertNotNull(volayaRepo.getId());
        assertNotNull(dblasbyRepo.getId());
        // the ids must have been assigned and so the forked from id based on the origin URI
        RepositoryInfo forkedFrom = dblasbyRepo.getForkedFrom();
        assertNotNull(forkedFrom);
        assertEquals(volayaRepo, forkedFrom);
        ////

        User admin = testSupport.getAdmin();
        volayaRepo.setOwner(new IdentifiedObject().id(user1.getId()));
        ReposClient adminReposClient = admin.repositories();
        Repo reassignedRepo1 = adminReposClient.modify(volayaRepo);

        dblasbyRepo.setOwner(new IdentifiedObject().id(user2.getId()));
        Repo reassignedRepo2 = adminReposClient.modify(dblasbyRepo);

        assertNotNull(reassignedRepo1);
        assertNotNull(reassignedRepo2);
        assertEquals("orphan1", reassignedRepo1.getIdentity());
        assertEquals("orphan2", reassignedRepo2.getIdentity());

        Repo user1Repo = user1.getRepo("orphan1");
        Repo user2Repo = user2.getRepo("orphan2");

    }

    private void setConfig(Repository repo, String key, String value) {
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName(key).setValue(value)
                .call();
    }

    private Repository clone(Repository origin, String cloneName) {
        Repository clone = createOrphan(cloneName);
        Remote remote = clone.command(RemoteAddOp.class).setName("origin")
                .setURL(origin.getLocation().toString()).call();
        PullResult pullResult = clone.command(PullOp.class)
                .setAuthor("testuser", "testuser@example.com").call();
        return clone;
    }

    private Repository createOrphan(String orphanName) {
        return createOrphan(orphanName, null, null);
    }

    private Repository createOrphan(String orphanName, @Nullable UUID previousId,
            @Nullable UUID previousForkedFrom) {

        FileStoreInfo connectionInfo = (FileStoreInfo) store.getInfo().getConnectionInfo();
        URI baseDirectoryURI = URI.create(connectionInfo.getDirectory());
        URI orphan1URI = RepositoryResolver.lookup(baseDirectoryURI).buildRepoURI(baseDirectoryURI,
                orphanName);
        Hints hints = Hints.readWrite().uri(orphan1URI);
        hints.set(Hints.REPOSITORY_NAME, orphanName);
        Context context = GlobalContextBuilder.builder().build(hints);
        Repository orphanRepo = context.command(InitOp.class).call();
        Optional<URI> call = orphanRepo.command(ResolveGeogigURI.class).call();
        assertEquals(orphanName, orphanRepo.command(ResolveRepositoryName.class).call());
        assertTrue(call.isPresent());
        if (previousId != null) {
            setConfig(orphanRepo, RepositoryManagementService.REPO_UUID_CONFIG_KEY,
                    previousId.toString());
        }
        if (previousForkedFrom != null) {
            setConfig(orphanRepo, RepositoryManagementService.REPO_FORKED_FROM_UUID_CONFIG_KEY,
                    previousForkedFrom.toString());
        }

        return orphanRepo;
    }

}
