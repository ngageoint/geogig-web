package org.geogig.server.service.repositories;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.RepoInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public @Service @Transactional class RepositoriesJPAStore implements RepositoryInfoStore {

    private @Autowired RepositoriesRepository repo;

    public @Override List<RepoInfo> findAll() {
        return Lists.newArrayList(repo.findAll());
    }

    public @Override RepoInfo create(RepoInfo repo) {
        checkMandatoryParams(repo);
        repo = this.repo.save(repo);
        return repo;
    }

    public @Override @Transactional RepoInfo save(RepoInfo repo) {
        checkMandatoryParams(repo);
        return this.repo.save(repo);
    }

    private void checkMandatoryParams(RepoInfo repo) {
        checkNotNull(repo);
        checkArgument(null != repo.getId(), "Repository id not provided");
        checkArgument(!Strings.isNullOrEmpty(repo.getIdentity()), "Repository name not provided");
        checkArgument(null != repo.getOwnerId(), "Owner not provided");
        checkArgument(null != repo.getStoreId(), "Store not provided");
    }

    public @Override List<RepoInfo> getByOwner(UUID ownerId) {
        return Lists.newArrayList(repo.findByOwnerId(ownerId));
    }

    public @Override Optional<RepoInfo> getByOwner(UUID id, String repoName) {
        return repo.findByOwnerIdAndIdentity(id, repoName);
    }

    public @Override List<RepoInfo> getByStore(UUID storeId) {
        return Lists.newArrayList(repo.findByStoreId(storeId));
    }

    public @Override int getCountByStore(UUID storeId) {
        return repo.countByStoreId(storeId);
    }

    public @Override void deleteById(UUID id) {
        repo.deleteById(id);
    }

    public @Override Optional<RepoInfo> findById(UUID id) {
        return repo.findById(id);
    }

    public @Override boolean existsById(UUID repoId) {
        return repo.existsById(repoId);
    }
}
