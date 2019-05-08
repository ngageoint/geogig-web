package org.geogig.server.stats.storage;

import java.util.UUID;

import org.geogig.server.model.RepoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public @Repository interface RepositoryStatsRepository extends JpaRepository<RepoStats, UUID> {
}
