package org.geogig.server.service.stores;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import lombok.NonNull;

public @Repository interface StoresRepository extends CrudRepository<Store, UUID> {

    public Optional<Store> findByIdentity(@NonNull String name);
    
    boolean existsByIdentity(String name);
}
