package org.geogig.server.service.stores;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.Store;
import org.springframework.stereotype.Service;

@Service("StoreInfoStore")
public interface StoreInfoStore {

    public Iterable<Store> findAll();

    public Optional<Store> findById(UUID id);

    public Optional<Store> findByName(String name);

    /**
     * @throws IllegalArgumentException if a store with the same id or name already exist.
     * @throws {@link NullPointerException} if a required attribute is not present (for instance,
     *         {@link StoreInfo#getId() id}, {@link StoreInfo#getIdentity() identity},
     *         {@link StoreInfo#getConnectionInfo() connectionInfo}, {@link StoreInfo#isEnabled()
     *         enabled}) .
     */
    public Store create(Store storeInfo);

    /**
     * @return 
     * @throws IllegalArgumentException if a store with the same name and different id already
     *         exist.
     * @throws {@link NullPointerException} if a required attribute is not present (for instance,
     *         {@link StoreInfo#getId() id}, {@link StoreInfo#getIdentity() identity},
     *         {@link StoreInfo#getConnectionInfo() connectionInfo}, {@link StoreInfo#isEnabled()
     *         enabled}) .
     */
    public Store modify(Store storeInfo);

    /**
     * @throws NoSuchElementException if a store with the provided id does not exist
     */
    public void remove(UUID id);

}
