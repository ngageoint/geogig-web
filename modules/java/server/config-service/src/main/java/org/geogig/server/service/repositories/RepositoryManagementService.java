package org.geogig.server.service.repositories;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.geogig.RepositoryStore;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.model.User;
import org.geogig.server.service.stores.StoreService;
import org.geogig.server.service.user.UserService;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("RepositoryManagementService")
public class RepositoryManagementService {
    public static final String REPO_UUID_CONFIG_KEY = "repo.uuid";

    public static final String REPO_OWNER_UUID_CONFIG_KEY = "repo.owner.uuid";

    public static final String REPO_FORKED_FROM_UUID_CONFIG_KEY = "repo.origin.uuid";

    private @Autowired UserService users;

    private @Autowired @Getter(value = AccessLevel.PACKAGE) StoreService stores;

    private @Autowired @Getter(value = AccessLevel.PACKAGE) RepositoryInfoStore infoStore;

    public Repository resolve(String user, String repo)
            throws NoSuchElementException, RepositoryConnectionException {

        final RepoInfo repoInfo = getByName(user, repo)
                .orElseThrow(() -> new NoSuchElementException("Repository not foud"));

        return resolve(repoInfo);
    }

    public Repository resolve(@NonNull UUID repoId) {
        return resolve(getOrFail(repoId));
    }

    public UUID resolveId(@NonNull String user, @NonNull String repo) {
        return getOrFail(user, repo).getId();
    }

    public Repository resolve(final RepoInfo repoInfo) {
        Repository repository;
        try {
            final UUID storeId = repoInfo.getStoreId();
            final UUID ownerId = repoInfo.getOwnerId();
            final User owner = users.getOrFail(ownerId);
            RepositoryStore provider = stores.connect(storeId);
            String ownerName = owner.getIdentity();
            String repositoryName = repoInfo.getIdentity();
            repository = provider.getRepoByName(compositeName(ownerName, repositoryName));
        } catch (RepositoryConnectionException | RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return repository;
    }

    public URI resolveRepositoryURI(@NonNull UUID repoId) {
        RepoInfo repoInfo = getOrFail(repoId);
        final User owner = users.getOrFail(repoInfo.getOwnerId());
        RepositoryStore provider;
        try {
            provider = stores.connect(repoInfo.getStoreId());
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        URI uri = provider
                .resolveRepoURI(compositeName(owner.getIdentity(), repoInfo.getIdentity()));
        return uri;
    }

    public RepoInfo getOrFail(@NonNull String ownerName, @NonNull String repoName)
            throws NoSuchElementException {

        final RepoInfo repo = getByName(ownerName, repoName)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Repository not found: %s/%s", ownerName, repoName)));
        return repo;
    }

    public Set<RepoInfo> getForksOf(@NonNull String ownerName, @NonNull String repoName,
            boolean recursive) {

        final RepoInfo repo = getOrFail(ownerName, repoName);

        return getForksOf(repo, recursive);
    }

    public Set<RepoInfo> getForksOf(@NonNull RepoInfo repo, boolean recursive) {
        return getForksOf(repo.getId(), recursive);
    }

    public Set<RepoInfo> getForksOf(@NonNull UUID repo, boolean recursive) {
        Set<RepoInfo> forks = infoStore.getForksOf(repo, recursive);
        return forks;
    }

    public Set<RepoInfo> getConstellationOf(@NonNull String ownerName, @NonNull String repoName) {

        final RepoInfo repo = getOrFail(ownerName, repoName);

        return getConstellationOf(repo.getId());
    }

    public Set<RepoInfo> getConstellationOf(@NonNull final UUID repoId) {
        Set<RepoInfo> constellation = infoStore.getConstellationOf(repoId);
        return constellation;
    }

    public Optional<RepoInfo> get(@NonNull UUID repoId) {
        return infoStore.findById(repoId);
    }

    public boolean exists(@NonNull UUID repoId) {
        return infoStore.existsById(repoId);
    }

    public RepoInfo getOrFail(@NonNull UUID repoId) {
        return infoStore.findById(repoId)
                .orElseThrow(() -> new NoSuchElementException("Repository not found: " + repoId));
    }

    public List<RepoInfo> getByUser(@NonNull String user) {
        User owner = users.resolveOrFail(user);
        List<RepoInfo> byOwner = infoStore.getByOwner(owner.getId());
        return byOwner;
    }

    public Optional<RepoInfo> getByName(@NonNull String user, @NonNull String repo) {
        User owner = users.resolveOrFail(user);
        return infoStore.getByOwner(owner.getId(), repo);
    }

    public Optional<RepoInfo> getByURI(@NonNull URI repoURI) {
        final String compositeName = RepositoryResolver.lookup(repoURI).getName(repoURI);
        // inverse of compositeName()
        List<String> decomposed = Splitter.on(':').omitEmptyStrings().splitToList(compositeName);
        if (2 == decomposed.size()) {
            String user = decomposed.get(0);
            String repo = decomposed.get(1);
            return getByName(user, repo);
        }
        log.info("Could not determine managed repository name for URI {}", repoURI);
        return Optional.empty();
    }

    public List<RepoInfo> getByUser(@NonNull UUID userId) {
        List<RepoInfo> byOwner = infoStore.getByOwner(userId);
        return byOwner;
    }

    public Iterable<RepoInfo> getAll() {
        return infoStore.findAll();
    }

    public List<RepoInfo> getByStore(@NonNull UUID storeId) {
        return infoStore.getByStore(storeId);
    }

    public int countByStore(@NonNull UUID storeId) {
        return infoStore.getCountByStore(storeId);
    }

    public List<RepoInfo> getOrphanByStore(@NonNull UUID storeId) {
        return new OrphanRepoFinder(this).findAll(storeId);
    }

    public Optional<RepoInfo> getOrphanByStore(@NonNull UUID storeId, @NonNull UUID repositoryId) {
        return new OrphanRepoFinder(this).findByStore(storeId, repositoryId);
    }

    public Optional<RepoInfo> findOrphanById(@NonNull UUID repositoryId) {
        return new OrphanRepoFinder(this).findById(repositoryId);
    }

    public RepoInfo remove(@NonNull String ownerName, @NonNull String repoName) {

        RepoInfo repo = getOrFail(ownerName, repoName);

        final UUID storeId = repo.getStoreId();
        try {
            RepositoryStore provider = stores.connect(storeId);
            provider.delete(compositeName(ownerName, repoName));
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        } finally {
            infoStore.deleteById(repo.getId());
        }
        return repo;
    }

    public RepoInfo remove(@NonNull UUID repoId) {

        final RepoInfo repo = getOrFail(repoId);
        final UUID storeId = repo.getStoreId();
        try {
            User owner = users.getOrFail(repo.getOwnerId());
            RepositoryStore provider = stores.connect(storeId);
            String compositeName = compositeName(owner, repo);
            provider.delete(compositeName);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        } finally {
            infoStore.deleteById(repo.getId());
        }
        return repo;
    }

    public RepoInfo create(@NonNull String ownerName, @NonNull String repoName,
            @Nullable String targetStoreName, String description) {
        final User owner = users.resolveOrFail(ownerName);

        return create(owner, repoName, targetStoreName, description, true);
    }

    public RepoInfo create(@NonNull User owner, @NonNull String repoName) {
        return create(owner, repoName, null, null, true);
    }

    public RepoInfo create(@NonNull User owner, @NonNull String repoName, String targetStoreName,
            String description, boolean enabled) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(repoName),
                "Repository name not provided");

        infoStore.getByOwner(owner.getId(), repoName).ifPresent((r) -> {
            throw new IllegalArgumentException(String.format("Repository '%s/%s' already exists",
                    owner.getIdentity(), repoName));
        });

        final UUID userDefaultStore = owner.getDefaultStore();
        final UUID storeIdentifier;
        if (null == targetStoreName) {
            storeIdentifier = userDefaultStore;
        } else {
            Store store = stores.getByName(targetStoreName).orElseThrow(
                    () -> new NoSuchElementException("Store " + targetStoreName + " not found"));
            storeIdentifier = store.getId();

            Set<UUID> additional = owner.getAdditionalStores();
            if (!Objects.equals(storeIdentifier, userDefaultStore)) {
                if (null == additional || !additional.contains(storeIdentifier)) {
                    throw new IllegalArgumentException(
                            "User is not allowed to use the requested store");
                }
            }
        }

        RepoInfo repoInfo = new RepoInfo();
        repoInfo.setId(UUID.randomUUID());
        repoInfo.setIdentity(repoName);
        repoInfo.setEnabled(enabled);
        repoInfo.setDescription(description);
        repoInfo.setOwnerId(owner.getId());
        repoInfo.setStoreId(storeIdentifier);

        RepositoryStore provider;

        try {
            provider = stores.connect(repoInfo.getStoreId());
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> config = buildRepoInitialConfig(owner, repoInfo);

        final String compositeName = compositeName(owner, repoInfo);
        Repository repo = provider.create(compositeName, config);
        try {
            URI repoUri = repo.command(ResolveGeogigURI.class).call().orNull();
            Preconditions.checkState(null != repoUri,
                    "Unable to resolve URI of newly created repository.");

            repoInfo = infoStore.create(repoInfo);
        } catch (RuntimeException e) {
            provider.delete(compositeName);
            throw e;
        } finally {

        }
        return repoInfo;
    }

    private Map<String, String> buildRepoInitialConfig(User owner, RepoInfo repoInfo) {
        // set author info, if provided in request parameters
        final String authorName = owner.getFullName() == null ? owner.getIdentity()
                : owner.getFullName();
        final @Nullable String authorEmail = owner.getEmailAddress() == null ? ""
                : owner.getEmailAddress();

        Map<String, String> config = ImmutableMap.of(//
                REPO_OWNER_UUID_CONFIG_KEY, owner.getId().toString(), //
                REPO_UUID_CONFIG_KEY, repoInfo.getId().toString(), //
                "user.name", authorName, //
                "user.email", authorEmail//
        );

        return config;
    }

    public String compositeName(RepoInfo repoInfo) {
        User owner = users.getOrFail(repoInfo.getOwnerId());
        return compositeName(owner, repoInfo);
    }

    private String compositeName(User owner, RepoInfo repoInfo) {
        String userName = owner.getIdentity();
        String repoName = repoInfo.getIdentity();
        return compositeName(userName, repoName);
    }

    private String compositeName(String userName, String repoName) {
        return String.format("%s:%s", userName, repoName);
    }

    //@formatter:off
    @Async("forksExecutor")
    public CompletableFuture<RepoInfo> fork(
            @NonNull User caller,
            @NonNull RepoInfo origin, 
            @NonNull User targetOwner, 
            @Nullable String targetName, 
            @Nullable String targetStore,
            @NonNull ProgressListener progress) {
      //@formatter:on

        targetName = targetName == null ? origin.getIdentity() : targetName;
        if (origin.getOwnerId().equals(targetOwner.getId())
                && origin.getIdentity().equals(targetName)) {

            throw new IllegalArgumentException("origin and target repos are the same");
        }
        User originOwner = users.getOrFail(origin.getOwnerId());
        log.info("Forking {}:{} to {}:{}...", originOwner.getIdentity(), origin.getIdentity(),
                targetOwner.getIdentity(), targetName);
        CompletableFuture<RepoInfo> c = new CompletableFuture<>();
        try {
            Repository originRepo = resolve(origin);
            URI originURI = originRepo.getLocation();
            RepoInfo cloneInfo = create(targetOwner, targetName, targetStore,
                    origin.getDescription(), true);

            cloneInfo.setCreatedBy(caller.getIdentity());
            cloneInfo.setForkedFrom(origin.getId());
            cloneInfo = infoStore.save(cloneInfo);

            Repository clone = resolve(cloneInfo);
            try {
                clone = clone.command(CloneOp.class)//
                        .setCloneIndexes(true)//
                        .setRemoteURI(originURI)//
                        // .setCloneURI(cloneURI)//
                        .setProgressListener(progress)//
                        .call();
                clone.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET)
                        .setName(REPO_FORKED_FROM_UUID_CONFIG_KEY)
                        .setValue(origin.getId().toString()).call();
                log.info("Fork successfull, cloning spatial indexes...");
            } catch (RuntimeException e) {
                e.printStackTrace();
                remove(targetOwner.getIdentity(), targetName);
                throw e;
            }
            // List<String> originSnapshot = createSnapshotInfo(origin, clone);
            // cloneInfo.setOriginSnapshot(originSnapshot);
            infoStore.save(cloneInfo);

            c.complete(cloneInfo);
            String msg = String.format("Clone successful: %s:%s to %s:%s",
                    originOwner.getIdentity(), origin.getIdentity(), targetOwner.getIdentity(),
                    targetName);
            progress.setProgressIndicator(p -> msg);
            log.info(msg);
        } catch (Exception e) {
            log.warn("Error cloning {}:{} to {}:{}", originOwner.getIdentity(),
                    origin.getIdentity(), targetOwner.getIdentity(), targetName, e);
            c.completeExceptionally(e);
        }
        return c;
    }

    public RepoInfo update(@NonNull RepoInfo repo) {
        checkRequiredProperties(repo);
        final RepoInfo current = getOrFail(repo.getId());
        final User owner = users.getOrFail(repo.getOwnerId());

        RepositoryStore repoStore;
        try {
            repoStore = stores.connect(repo.getStoreId());

            Map<String, String> config = new HashMap<>();
            User oldOwner = owner;
            if (!repo.getOwnerId().equals(current.getOwnerId())) {
                oldOwner = users.getOrFail(current.getOwnerId());
                config.put(REPO_OWNER_UUID_CONFIG_KEY, owner.getId().toString());
                config.put("user.name",
                        owner.getFullName() == null ? owner.getIdentity() : owner.getFullName());
                config.put("user.email", owner.getEmailAddress());
            }
            if (!repo.getIdentity().equals(current.getIdentity())) {
                String repoName = repo.getIdentity();
                config.put("repo.name", compositeName(owner.getIdentity(), repoName));
            }
            if (!config.isEmpty()) {
                String oldCompositeName = compositeName(oldOwner.getIdentity(),
                        current.getIdentity());
                repoStore.setConfig(oldCompositeName, config);
            }
            RepoInfo saved = this.infoStore.save(repo);
            return saved;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    public RepoInfo updateOrphan(@NonNull RepoInfo repo) {
        checkRequiredProperties(repo);

        final User owner = users.getOrFail(repo.getOwnerId());

        RepositoryStore repoStore;
        RepoInfo currentOrphan;
        try {
            repoStore = stores.connect(repo.getStoreId());
            currentOrphan = new OrphanRepoFinder(this).findByStore(repo.getStoreId(), repo.getId())
                    .orElseThrow(() -> new NoSuchElementException(String.format(
                            "Orphan repo %s (%s) not found", repo.getIdentity(), repo.getId())));
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        // remove old name prefix if needed
        String name = repo.getIdentity();
        int splitIndex;
        if ((splitIndex = name.indexOf(':')) > 0) {
            name = name.substring(splitIndex + 1);
            repo.setIdentity(name);
        }

        Optional<RepoInfo> existent = this.infoStore.getByOwner(owner.getId(), repo.getIdentity());
        if (existent.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Repository %s already exists for this user, try a different name",
                    repo.getIdentity()));
        }
        this.infoStore.create(repo);

        try {
            Map<String, String> config = new HashMap<>();
            config.put(REPO_OWNER_UUID_CONFIG_KEY, owner.getId().toString());
            String repoName = repo.getIdentity();
            config.put("repo.name", compositeName(owner.getIdentity(), repoName));
            config.put("user.name",
                    owner.getFullName() == null ? owner.getIdentity() : owner.getFullName());
            config.put("user.email", owner.getEmailAddress());

            repoStore.setConfig(currentOrphan.getIdentity(), config);
            return repo;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkRequiredProperties(RepoInfo repo) {
        Preconditions.checkNotNull(repo.getId(), "id not provided");
        Preconditions.checkNotNull(repo.getStoreId(), "store not provided");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(repo.getIdentity()),
                "repository name not provided");
        Preconditions.checkNotNull(repo.getOwnerId(), "owner not provided");
    }

}
