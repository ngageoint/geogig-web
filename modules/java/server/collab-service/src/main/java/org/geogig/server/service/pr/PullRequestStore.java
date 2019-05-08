package org.geogig.server.service.pr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.PullRequest;
import org.springframework.stereotype.Service;

@Service("PullRequestStore")
public interface PullRequestStore {

    public List<PullRequest> getAll();

    public Iterable<PullRequest> getByTargetRepository(UUID repoId);

    public Optional<PullRequest> get(UUID repoId, int prid);

    public void remove(UUID repoId, int prid);

    public PullRequest create(PullRequest PullRequest);

    public PullRequest modify(PullRequest PullRequest);

    Iterable<PullRequest> findAll();

}
