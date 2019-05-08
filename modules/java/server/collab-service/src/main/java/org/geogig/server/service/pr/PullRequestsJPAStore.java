package org.geogig.server.service.pr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.PullRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import lombok.NonNull;

public @Service class PullRequestsJPAStore implements PullRequestStore {

    private @Autowired PullRequestsRepository repo;

    public @Override PullRequest create(@NonNull PullRequest PullRequest) {
        return repo.save(PullRequest);
    }

    public @Override PullRequest modify(@NonNull PullRequest PullRequest) {
        return repo.save(PullRequest);
    }

    public @Override List<PullRequest> getAll() {
        return Lists.newArrayList(repo.findAll());
    }

    public @Override Iterable<PullRequest> findAll() {
        return repo.findAll();
    }

    public @Override Iterable<PullRequest> getByTargetRepository(UUID repoId) {
        return repo.findByRepositoryId(repoId);
    }

    public @Override Optional<PullRequest> get(UUID repoId, int prid) {
        return repo.findById(prid);
    }

    public @Override void remove(UUID repoId, int prid) {
        repo.deleteByIdAndRepositoryId(prid, repoId);
    }
}
