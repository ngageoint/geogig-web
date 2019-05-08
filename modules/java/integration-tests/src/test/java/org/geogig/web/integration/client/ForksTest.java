package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.AsyncTask;
import org.geogig.web.client.Repo;
import org.geogig.web.client.ReposClient;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RepositoryInfo;
import org.junit.Before;
import org.junit.Test;

public class ForksTest extends AbstractIntegrationTest {

    private Store store;

    private User gabe, dave;

    private Repo gabesRepo;

    private String storeName;

    private void log(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }

    public @Before void before() {
        storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        gabe = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);
        dave = testSupport.createUser("dave", "s3cr3t", "David Blasby", "dave@example.com",
                storeName);

        gabesRepo = gabe.createRepo("naturalEearth");
        createPoiLayer(gabesRepo);
    }

    private User createTestUser(String name) {
        User u = testSupport.createUser(name, "s3cr3t", "User " + name, name + "@example.com",
                storeName);
        return u;
    }

    public @Test void forkRepository() throws Exception {
        Repo origin = gabesRepo;
        Repo fork = fork(origin, dave, null);
        assertNotNull(fork);
    }

    public @Test void getConstellation() throws Exception {
        Repo root1 = gabesRepo;
        Repo root2 = dave.createRepo("davesrepo");
        createPoiLayer(root2);
        User u1 = createTestUser("u1");
        User u2 = createTestUser("u2");
        User u3 = createTestUser("u3");
        User u4 = createTestUser("u4");
        User u5 = createTestUser("u5");

        Repo f11 = fork(root1, u1, "f11");
        Repo f12 = fork(root1, u2, "f12");
        Repo f111 = fork(f11, u3, "f111");
        Repo f121 = fork(f12, u4, "f121");
        Repo f122 = fork(f121, u5, "f122");

        Repo f21 = fork(root2, u1, "f21");
        Repo f22 = fork(root2, u2, "f22");
        Repo f211 = fork(f21, u3, "f211");
        Repo f221 = fork(f22, u4, "f221");
        Repo f222 = fork(f221, u5, "f222");

        List<Repo> constellation1 = Arrays.asList(root1, f11, f12, f111, f121, f122);
        List<Repo> constellation2 = Arrays.asList(root2, f21, f22, f211, f221, f222);

        assertConstellation(constellation1, root1);
        assertConstellation(constellation1, f11);
        assertConstellation(constellation1, f12);
        assertConstellation(constellation1, f111);
        assertConstellation(constellation1, f121);
        assertConstellation(constellation1, f122);

        assertConstellation(constellation2, root2);
        assertConstellation(constellation2, f21);
        assertConstellation(constellation2, f22);
        assertConstellation(constellation2, f211);
        assertConstellation(constellation2, f221);
        assertConstellation(constellation2, f222);
    }

    private void assertConstellation(List<Repo> expected, Repo repo) {
        List<Repo> constellation = repo.getConstellation();
        assertNotNull(constellation);
        assertEquals(expected.size(), constellation.size());

        for (Repo expectedRepo : expected) {
            Optional<Repo> actual = constellation.stream()
                    .filter(r -> r.getId().equals(expectedRepo.getId())).findFirst();
            assertTrue("Repo not found: " + expectedRepo.getIdentity(), actual.isPresent());
        }
    }

    public @Test void forkRepositoryWithNewName() throws Exception {
        Repo origin = gabesRepo;
        Repo fork = fork(origin, dave, "myForkName");
        assertNotNull(fork);
    }

    private Repo fork(Repo origin, User forkOwner, @Nullable final String targetRepoName) {
        ReposClient targetReposClient = forkOwner.repositories();

        final String forkName = targetRepoName == null ? origin.getIdentity() : targetRepoName;

        log("Called fork on %s:%s to %s:%s, awaiting task termination...",
                origin.getOwner().getIdentity(), origin.getIdentity(), forkOwner.getIdentity(),
                forkName);

        AsyncTask<RepositoryInfo> forkTask;
        forkTask = targetReposClient.fork(origin, targetRepoName, null);

        forkTask.awaitTermination();
        {
            String msg = String.format("Fork task finished abnormally: %s, %s",
                    forkTask.getStatus(), forkTask.getInfo());
            assertTrue(msg, forkTask.isComplete());
        }
        assertEquals(StatusEnum.COMPLETE, forkTask.getStatus());
        AsyncTaskInfo taskInfo = forkTask.getInfo();
        assertNotNull(taskInfo.getStartedBy());
        assertEquals(forkOwner.getIdentity(), taskInfo.getStartedBy().getIdentity());
        assertNotNull(taskInfo.getScheduledAt());
        assertNotNull(taskInfo.getStartedAt());
        assertNotNull(taskInfo.getFinishedAt());
        assertNotNull(taskInfo.getLastUpdated());
        assertNull(taskInfo.getAbortedBy());
        assertNull(taskInfo.getTransaction());

        RepositoryInfo cloneInfo = forkTask.getResult();
        assertNotNull(cloneInfo);

        assertEquals(forkName, cloneInfo.getIdentity());
        IdentifiedObject owner = cloneInfo.getOwner();
        assertEquals(forkOwner.getIdentity(), owner.getIdentity());
        assertEquals(forkOwner.getId(), owner.getId());
        assertNotNull(cloneInfo.getStore());
        RepositoryInfo forkedFrom = cloneInfo.getForkedFrom();
        assertNotNull(forkedFrom);
        assertEquals(origin.getIdentity(), forkedFrom.getIdentity());
        assertEquals(origin.getId(), forkedFrom.getId());
        Repo repo = forkOwner.getRepo(forkName);
        return repo;
    }
}
