package org.geogig.server.app.gateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.service.auth.AuthenticationService;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.stores.StoreService;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.server.api.RepositoryStoresApi;
import org.geogig.web.server.api.RepositoryStoresApiDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link RepositoryStoresApi},
 * handles the REST request/response/error handling aspects of the API, and delegates business logic
 * to a {@link StoreService}.
 */
public @Service class StoresServiceApi extends AbstractService
        implements RepositoryStoresApiDelegate {

    private @Autowired StoreService stores;

    private @Autowired AuthenticationService auth;

    private @Autowired PresentationService presentation;

    @Autowired
    private RepositoryManagementService repositories;

    public @Override ResponseEntity<List<StoreInfo>> getStores() {
        final boolean summaryOnly = !auth.currentUserIsAdmin();

        Stream<Store> all = Streams.stream(stores.getAll());
        Stream<StoreInfo> infos = all.map(s -> presentation.toInfo(s, summaryOnly));
        return super.ok(infos.collect(Collectors.toList()));
    }

    public @Override ResponseEntity<StoreInfo> getStore(@NonNull String name) {
        final boolean summaryOnly = !auth.currentUserIsAdmin();
        Optional<Store> store = stores.getByName(name);
        return okOrNotFound(store.map(s -> presentation.toInfo(s, summaryOnly)));
    }

    /**
     * <pre>
     * <code>
     * {
     *   "identity": "postgres",
     *   "description": "Default postgresql",
     *   "enabled": true,
     *   "connectionInfo": {
     *     "type": "PostgresStoreInfo",
     *     "server": "localhost",
     *     "port": 5432,
     *     "database": "geogig_web",
     *     "schema": "public",
     *     "user": "postgres",
     *     "password": "geo123"
     *   }
     * }
     * </code>
     * </pre>
     * 
     */
    public @Override ResponseEntity<StoreInfo> createStore(@NonNull StoreInfo storeInfo) {
        auth.requireAdmin();
        Store store = presentation.toModel(storeInfo);
        return super.create(() -> presentation.toInfo(stores.create(store)));
    }

    public @Override ResponseEntity<Void> removeStore(@NonNull String store) {
        auth.requireAdmin();
        return super.run(HttpStatus.NO_CONTENT, (Runnable) (() -> stores.removeByName(store)));
    }

    public @Override ResponseEntity<StoreInfo> modifyStore(@NonNull StoreInfo storeInfo) {
        auth.requireAdmin();
        Store store = presentation.toModel(storeInfo);
        return super.run(HttpStatus.OK, () -> presentation.toInfo(stores.modify(store)));
    }

    public @Override ResponseEntity<List<RepositoryInfo>> listStoreRepos(
            @NonNull String storeName) {
        Store store = stores.getByNameOrFail(storeName);
        final UUID storeId = store.getId();
        List<RepoInfo> repos = repositories.getByStore(storeId);
        return super.ok(Lists.transform(repos, r -> presentation.toInfo(r)));
    }

    public @Override ResponseEntity<List<RepositoryInfo>> listStoreOrphanRepos(
            @NonNull String storeName) {

        Store store = stores.getByNameOrFail(storeName);
        final UUID storeId = store.getId();
        List<RepoInfo> repos = repositories.getOrphanByStore(storeId);
        return super.ok(Lists.transform(repos, r -> presentation.toInfo(r)));
    }

    public @Override ResponseEntity<Integer> countStoreRepos(@NonNull String storeName) {
        Optional<Store> store = stores.getByName(storeName);
        if (!store.isPresent()) {
            return notFound("Store %s not found", storeName);
        }
        final UUID storeId = store.get().getId();
        return super.ok(repositories.countByStore(storeId));
    }

}
