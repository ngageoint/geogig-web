package org.geogig.server.service.repositories;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.geogig.RepositoryStore;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.service.stores.StoreService;
import org.locationtech.geogig.plumbing.remotes.RemoteResolve;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
class OrphanRepoFinder {
    private RepositoryManagementService service;

    public Optional<RepoInfo> findById(@NonNull UUID repositoryId) {
        Iterable<Store> stores = service.getStores().getAll();

        Optional<RepoInfo> repo = Optional.empty();
        for (Store store : stores) {
            if (!store.isEnabled()) {
                continue;
            }
            repo = findByStore(store.getId(), repositoryId);
            if (repo.isPresent()) {
                break;
            }
        }
        return repo;
    }

    public Optional<RepoInfo> findByStore(@NonNull UUID storeId, @NonNull UUID repositoryId) {
        Optional<RepoInfo> orphan = Optional.empty();
        RepositoryStore repoProvider;
        try {
            repoProvider = service.getStores().connect(storeId);
            List<String> allNames = repoProvider.getAllNames();
            for (String name : allNames) {
                Repository repo = repoProvider.getRepoByName(name);
                Optional<UUID> id = getOrAssignRepoUUID(Optional.of(repo));
                if (id.isPresent() && id.get().equals(repositoryId)) {
                    Optional<UUID> forkedFromUUID = resolveForkedFromUUID(repo);
                    orphan = Optional.of(toOrphan(name, storeId, id.get(), forkedFromUUID));
                }
            }
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        return orphan;
    }

    public List<RepoInfo> findAll(@NonNull UUID storeId) {
        StoreService stores = service.getStores();

        RepositoryStore repoProvider;
        try {
            repoProvider = stores.connect(storeId);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }

        List<RepoInfo> orphans = new ArrayList<>();
        List<String> allRepoNames = repoProvider.getAllNames();
        for (String reponame : allRepoNames) {
            Optional<Repository> repository = Optional.empty();
            Optional<UUID> repoUUID = Optional.empty();
            Optional<UUID> forkedFromUUID = Optional.empty();

            repository = load(reponame, repoProvider);
            repoUUID = getOrAssignRepoUUID(repository);
            forkedFromUUID = getForkedFrom(repository);
            if (repoUUID.isPresent()) {
                if (service.exists(repoUUID.get())) {
                    continue;
                }
            }

            if (repository.isPresent()) {
                Preconditions.checkState(repoUUID.isPresent());
                UUID id = repoUUID.get();
                if (!forkedFromUUID.isPresent()) {
                    Repository r = repository.get();
                    forkedFromUUID = resolveForkedFromUUID(r);
                }
                orphans.add(toOrphan(reponame, storeId, id, forkedFromUUID));
            }
        }
        return orphans;
    }

    private Optional<UUID> resolveForkedFromUUID(Repository r) {
        Optional<UUID> forkedFromUUID = Optional.empty();
        Remote origin = r.command(RemoteResolve.class).setName("origin").call().orNull();
        if (origin != null) {
            Optional<Repository> originrepo = resolveByURI(origin.getFetchURL());
            if (originrepo.isPresent()) {
                forkedFromUUID = getOrAssignRepoUUID(originrepo);
                originrepo.get().close();
                forkedFromUUID.ifPresent(uuid -> setForkedFrom(r, uuid));
            }
        }
        return forkedFromUUID;
    }

    private Optional<Repository> resolveByURI(@NonNull String fetchURL) {
        try {
            Repository repo = RepositoryResolver.load(URI.create(fetchURL));
            return Optional.of(repo);
        } catch (Exception e) {
            log.warn("Unable to load repository by URI: {}", fetchURL, e);
            return Optional.empty();
        }
    }

    private Optional<Repository> load(String reponame, RepositoryStore repoProvider) {
        try {
            return Optional.of(repoProvider.getRepoByName(reponame));
        } catch (RepositoryConnectionException e) {
            return Optional.empty();
        }
    }

    private Optional<UUID> getOrAssignRepoUUID(Optional<Repository> repo) {
        Optional<UUID> uuid = Optional.empty();
        if (repo.isPresent()) {
            String uuidKey = RepositoryManagementService.REPO_UUID_CONFIG_KEY;
            uuid = getUUIDFromConfig(repo, uuidKey);
            if (!uuid.isPresent()) {
                UUID id = UUID.randomUUID();
                repo.get().command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET)
                        .setName(RepositoryManagementService.REPO_UUID_CONFIG_KEY)
                        .setValue(id.toString()).call();
                log.info("Assigned UUID {} to orphan repo {}", id, repo.get().getLocation());
                uuid = Optional.of(id);
            }
        }
        return uuid;
    }

    private void setForkedFrom(Repository repo, UUID forkedFrom) {

        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET)
                .setName(RepositoryManagementService.REPO_FORKED_FROM_UUID_CONFIG_KEY)
                .setValue(forkedFrom.toString()).call();
    }

    private Optional<UUID> getForkedFrom(Optional<Repository> repo) {
        String uuidKey = RepositoryManagementService.REPO_FORKED_FROM_UUID_CONFIG_KEY;
        return getUUIDFromConfig(repo, uuidKey);
    }

    private Optional<UUID> getUUIDFromConfig(Optional<Repository> repo, String uuidKey) {
        Optional<String> storedId = repo.flatMap(r -> {
            return com.google.common.base.Optional.toJavaUtil(r.configDatabase().get(uuidKey));
        });
        if (storedId.isPresent()) {
            try {
                UUID uuid = UUID.fromString(storedId.get());
                return Optional.of(uuid);
            } catch (IllegalArgumentException ignore) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private RepoInfo toOrphan(String name, UUID storeId, UUID id, Optional<UUID> forkedFromUUID) {

        RepoInfo orphan = new RepoInfo();
        orphan.setId(id);
        orphan.setStoreId(storeId);
        orphan.setIdentity(name);
        orphan.setOwnerId(null);
        orphan.setDeleted(false);
        orphan.setForkedFrom(forkedFromUUID.orElse(null));
//        Instant now = Instant.now();
//        orphan.setCreatedAt(now);
//        orphan.setUpdatedAt(now);
        return orphan;
    }
}
