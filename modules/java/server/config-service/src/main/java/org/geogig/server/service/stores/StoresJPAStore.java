package org.geogig.server.service.stores;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

import lombok.NonNull;

public @Service class StoresJPAStore implements StoreInfoStore {

    private @Autowired StoresRepository repo;

    public @Override Optional<Store> findByName(String name) {
        return repo.findByIdentity(name);
    }

    public @Override Store create(@NonNull Store store) {
        Preconditions.checkArgument(store.getId() == null);
        if (repo.existsByIdentity(store.getIdentity())) {
            throw new IllegalArgumentException("Store " + store.getIdentity() + " already exists");
        }
        store.setId(UUID.randomUUID());
        Store saved = repo.save(store);
        return saved;
    }

    public @Override Store modify(Store store) {
        return repo.save(store);
    }

    public @Override void remove(UUID id) {
        repo.deleteById(id);
    }

    public @Override Iterable<Store> findAll() {
        return repo.findAll();
    }

    public @Override Optional<Store> findById(UUID id) {
        return repo.findById(id);
    }
}
