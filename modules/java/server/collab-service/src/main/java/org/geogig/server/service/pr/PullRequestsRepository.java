package org.geogig.server.service.pr;

import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.geogig.server.model.PullRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import lombok.NonNull;

public @Repository interface PullRequestsRepository extends CrudRepository<PullRequest, Integer> {

    public Iterable<PullRequest> findByRepositoryId(@NonNull UUID repoId);

    public Optional<PullRequest> findByIdAndRepositoryId(int prid, UUID repoId);

    public @Transactional void deleteByIdAndRepositoryId(int prid, UUID repoId);
}
