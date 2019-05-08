package org.geogig.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.model.User;
import org.geogig.server.model.User.UserType;
import org.geogig.server.service.auth.AuthenticationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.stores.StoreService;
import org.geogig.server.service.user.UserService;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.NonNull;

public @Service class ServiceTestSupport extends ExternalResource {

    // private @Autowired TestOnlyUserDetailsManager userDetailsManager;

    private @Autowired @Getter AuthenticationService auth;

    private @Autowired @Getter StoreService stores;

    private @Autowired @Getter UserService users;

    private @Autowired @Getter RepositoryManagementService repos;

    private TemporaryFolder tmp = new TemporaryFolder();

    private @Getter Store defaultStore;

    protected @Override void before() throws Exception {
        tmp.create();
        defaultStore = createStore("default-store");
    }

    protected @Override void after() {
        runAs("admin");
        for (RepoInfo repo : repos.getAll()) {
            repos.get(repo.getId()).ifPresent(r -> repos.remove(r.getId()));
        }
        for (User user : users.getAll()) {
            users.deleteById(user.getId());
        }
        for (Store store : stores.getAll()) {
            stores.removeById(store.getId());
        }
        tmp.delete();
    }

    public ServiceTestSupport runAs(@NonNull User user) {
        return runAs(user.getIdentity());
    }

    public ServiceTestSupport runAs(@NonNull String user) {
        SecurityContext context = SecurityContextHolder.getContext();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(user, null);
        context.setAuthentication(authentication);
        return this;
    }

    private String getCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        assertTrue(authentication instanceof TestingAuthenticationToken);
        return ((TestingAuthenticationToken) authentication).getName();
    }

    public Store createStore(@NonNull String name) {
        runAs("admin");
        String baseURI;
        try {
            baseURI = tmp.newFolder("store-" + name).toURI().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Store info = Store.builder().identity(name).enabled(true).baseURI(baseURI).build();
        Store created = stores.create(info);
        return created;
    }

    public User createUser(@NonNull String login) {
        return createUser(login, defaultStore);
    }

    public User createUser(@NonNull String login, @NonNull Store defaultStore,
            @Nullable Store... additionalStores) {
        runAs("admin");
        User info = new User();
        info.setIdentity(login);
        info.setFullName(login + "'s full name");
        info.setEmailAddress(login + "@example.com");
        info.setType(UserType.INDIVIDUAL);
        info.setDefaultStore(defaultStore.getId());
        if (additionalStores != null) {
            for (Store st : additionalStores) {
                info.getAdditionalStores().add(st.getId());
            }
        }

        User created = users.create(info);
        assertNotNull(created);
        assertEquals(created, users.getByNameOrFail(login));
        assertEquals(created, users.getOrFail(created.getId()));
        HashSet<User> all = Sets.newHashSet(users.getAll());
        assertTrue(all.contains(created));
        auth.setPassword(created.getIdentity(), "geo123");

        return created;
    }

    public RepoInfo createRepo(@NonNull String repoName) {
        String owner = getCurrentUser();
        return createRepo(owner, repoName, defaultStore);
    }

    public RepoInfo createRepo(@NonNull User owner, @NonNull String repoName) {
        String user = owner.getIdentity();
        UUID storeid = owner.getDefaultStore();
        Store store = getStore(storeid);
        return createRepo(user, repoName, store);
    }

    public RepoInfo createRepo(@NonNull String owner, @NonNull String repoName) {
        return createRepo(owner, repoName, defaultStore);
    }

    public RepoInfo createRepo(@NonNull String owner, @NonNull String repoName,
            @NonNull Store store) {
        runAs(owner);
        RepoInfo repo = repos.create(owner, repoName, store.getIdentity(), null);
        return repo;
    }

    public RepoInfo fork(@NonNull RepoInfo origin) {
        String owner = getUser(origin.getOwnerId()).getIdentity();
        String repoName = origin.getIdentity();
        return fork(owner, repoName);
    }

    public RepoInfo fork(@NonNull String owner, @NonNull String repoName) {
        return forkAs(owner, repoName, repoName);
    }

    public RepoInfo forkAs(@NonNull String owner, @NonNull String repoName,
            @NonNull String forkName) {

        String caller = getCurrentUser();
        RepoInfo origin = repos.getOrFail(owner, repoName);
        User targetOwner = users.getByNameOrFail(caller);
        String targetStore = null;
        CompletableFuture<RepoInfo> fork;
        fork = repos.fork(targetOwner, origin, targetOwner, forkName, targetStore,
                new DefaultProgressListener());
        try {
            return fork.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Repository getRepository(@NonNull RepoInfo info) {
        Repository repo = repos.resolve(info);
        return repo;
    }

    public RepoInfo getRepositoryInfo(@NonNull UUID repoId) {
        RepoInfo repo = repos.getOrFail(repoId);
        return repo;
    }

    public User getUser(@NonNull UUID id) {
        return users.getOrFail(id);
    }

    public Store getStore(@NonNull String storeName) {
        return stores.getByNameOrFail(storeName);
    }

    public Store getStore(@NonNull UUID id) {
        return stores.getOrFail(id);
    }

    public RepoInfo update(RepoInfo repo) {
        return repos.update(repo);
    }

    public RepoInfo delete(@NonNull RepoInfo repo) {
        return repos.remove(repo.getId());
    }

    public User update(@NonNull User user) {
        return users.modify(user);
    }

    public User delete(@NonNull User user) {
        repos.getByUser(user.getId()).forEach(r -> repos.remove(r.getId()));
        return users.deleteById(user.getId());
    }

    public List<RepoInfo> getRepos(@NonNull User user) {
        return repos.getByUser(user.getId());
    }

    public Store update(Store store) {
        return stores.modify(store);
    }

    public Store delete(Store store) {
        return stores.removeById(store.getId());
    }
}
