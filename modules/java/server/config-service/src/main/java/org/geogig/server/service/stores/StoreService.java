package org.geogig.server.service.stores;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.geogig.server.geogig.GeogigRepositoryProvider;
import org.geogig.server.geogig.RepositoryStore;
import org.geogig.server.model.Store;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import lombok.NonNull;

/**
 * Service to manage {@link Store repository stores}
 */
@Service("StoreService")
public class StoreService {

    private @Autowired GeogigRepositoryProvider repositoryProvider;

    private @Autowired StoreInfoStore stores;

    public RepositoryStore connect(@NonNull String storeName)
            throws NoSuchElementException, RepositoryConnectionException {

        final Store store = stores.findByName(storeName).orElseThrow(//
                () -> new NoSuchElementException(
                        String.format("Store named %s does not exist", storeName)));

        return connect(store);
    }

    /**
     * @param storeId
     * @return
     * @throws NoSuchElementException
     * @throws RepositoryConnectionException
     */
    public RepositoryStore connect(@NonNull UUID storeId)
            throws NoSuchElementException, RepositoryConnectionException {

        final Store store = stores.findById(storeId).orElseThrow(//
                () -> new NoSuchElementException(
                        String.format("Store with id %s does not exist", storeId)));

        return connect(store);
    }

    private RepositoryStore connect(Store store) throws RepositoryConnectionException {
        if (!Boolean.TRUE.equals(store.isEnabled())) {
            throw new RepositoryConnectionException("Repository store is disabled");
        }

        final RepositoryStore repoProvider = repositoryProvider.connect(store.getId(),
                store.getBaseURI());

        return repoProvider;
    }

    private RepositoryStore connectOrCreate(Store store)
            throws IllegalArgumentException, RepositoryConnectionException {

        final RepositoryStore repoProvider = repositoryProvider.connectOrCreate(store.getId(),
                store.getBaseURI());

        return repoProvider;
    }

    public Iterable<Store> getAll() {
        Iterable<Store> all = stores.findAll();
        return all;
    }

    public Store getOrFail(@NonNull UUID storeId) {
        return stores.findById(storeId).orElseThrow(
                () -> new NoSuchElementException(String.format("Store %s not found", storeId)));
    }

    public Optional<Store> get(@NonNull UUID storeId) {
        return stores.findById(storeId);
    }

    public Optional<Store> getByName(@NonNull String name) {
        return stores.findByName(name);
    }

    public Store getByNameOrFail(@NonNull String identity) {
        return getByName(identity).orElseThrow(
                () -> new NoSuchElementException(String.format("Store %s not found", identity)));
    }

    public Store create(@NonNull Store info) {
        checkMandatoryFields(info);
        checkArgument(null == info.getId(), "StoreInfo should not contain an id: %s", info.getId());

        info = stores.create(info);

        if (info.isEnabled()) {
            try {
                connectOrCreate(info);
            } catch (Exception ce) {
                stores.remove(info.getId());
                Throwables.throwIfUnchecked(ce);
                throw new RuntimeException(ce);
            }
        }
        return info;
    }

    public Store removeByName(@NonNull String storeName) {
        Store store = stores.findByName(storeName).orElseThrow(
                () -> new NoSuchElementException("Store does not exist: " + storeName));
        return remove(store);
    }

    private Store remove(@NonNull Store store) {
        UUID id = store.getId();
        stores.remove(id);
        return store;
    }

    public Store removeById(@NonNull UUID id) {
        Store store = stores.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Store does not exist: " + id));
        return remove(store);
    }

    public @Transactional Store modify(@NonNull Store info) {
        checkMandatoryFields(info);
        checkArgument(null != info.getId(), "Store id not provided");

        final Store oldInfo = stores.findById(info.getId()).orElse(null);
        checkArgument(null != oldInfo, "Store %s does not exist", info.getId());

        if (info.isEnabled()) {
            try {
                connectOrCreate(info);
            } catch (RepositoryConnectionException ce) {
                throw new RuntimeException(ce);
            }
        } else {
            repositoryProvider.invalidate(info.getId());
        }
        oldInfo.setDescription(info.getDescription());
        try {
            info = stores.modify(oldInfo);
        } catch (RuntimeException e) {
            repositoryProvider.invalidate(info.getId());
            throw e;
        }

        return info;
    }

    private void checkMandatoryFields(@NonNull Store info) {

        checkArgument(!Strings.isNullOrEmpty(info.getIdentity()), "Store name not provided");
        checkArgument(null != info.getBaseURI(), "Connection parameters not provided");
    }
}
