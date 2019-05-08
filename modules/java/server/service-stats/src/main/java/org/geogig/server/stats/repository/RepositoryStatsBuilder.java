package org.geogig.server.stats.repository;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geogig.server.model.PullRequest;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.RepoStats;
import org.geogig.server.service.pr.PullRequestService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.stats.StatsEvent;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.porcelain.BranchListOp;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public @Service class RepositoryStatsBuilder {

    private @Autowired RepositoryManagementService repos;

    private @Autowired RepositoryStatsService statsService;

    private @Autowired PullRequestService prService;

    private ConcurrentMap<UUID, RepoInfo> running = new ConcurrentHashMap<>();

    public @Subscribe void onRepoStatsMissing(StatsEvent.Missing event) {
        if (!(event.getSource() instanceof RepoInfo)) {
            return;
        }
        RepoInfo repo = (RepoInfo) event.getSource();
        if (null != running.putIfAbsent(repo.getId(), repo)) {
            log.debug("Initial stats for repo already running, ignoring event {}", event);
            return;
        }
        log.info("Building initial stats for " + repo);
        try {
            RepoStats stats = computeStats(repo);
            statsService.save(stats);
        } finally {
            running.remove(repo.getId());
        }
    }

    public RepoStats computeStats(@NonNull RepoInfo repo) {
        RepoStats stats = new RepoStats();
        stats.setId(repo.getId());
        Repository repository = repos.resolve(repo);
        ImmutableList<Ref> branches = repository.command(BranchListOp.class).call();
        stats.setNumBranches(branches.size());
        Set<RepoInfo> forks = repos.getForksOf(repo, true);
        stats.setNumForks(forks.size());

        Iterable<PullRequest> allrepoprs = prService.getByRepository(repo.getId(), true, true);
        Stream<PullRequest> prs = StreamSupport.stream(allrepoprs.spliterator(), false);
        long openprs = prs.filter(pr -> pr.isOpen()).count();

        prs = StreamSupport.stream(allrepoprs.spliterator(), false);
        long closedprs = prs.filter(pr -> pr.isClosed() || pr.isMerged()).count();

        stats.setNumPullRequestsClosed((int) closedprs);
        stats.setNumPullRequestsOpen((int) openprs);

        return stats;
        // no easy way to compute num commits when there are a large number yet
        // stats.setNumCommits(numCommits);
    }
}
