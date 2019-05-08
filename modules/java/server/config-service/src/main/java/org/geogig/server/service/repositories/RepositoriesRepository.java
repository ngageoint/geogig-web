package org.geogig.server.service.repositories;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.RepoInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;

public @Repository @Transactional interface RepositoriesRepository
        extends JpaRepository<RepoInfo, UUID> {

    public Optional<RepoInfo> findByIdentity(@NonNull String name);

    public Optional<RepoInfo> findByOwnerIdAndIdentity(@NonNull UUID ownerId, @NonNull String name);

    public Iterable<RepoInfo> findByOwnerId(@NonNull UUID owner);

    public Iterable<RepoInfo> findByStoreId(@NonNull UUID store);

    public int countByStoreId(UUID storeId);
}
