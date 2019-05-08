package org.geogig.server.stats;

import java.util.Optional;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.RepoStats;
import org.geogig.server.stats.repository.RepositoryStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;

public @Service class StatsService {

    private @Autowired RepositoryStatsService repoStats;

    public Optional<RepoStats> getRepoStats(@NonNull RepoInfo repo) {
        return repoStats.get(repo);
    }
}
