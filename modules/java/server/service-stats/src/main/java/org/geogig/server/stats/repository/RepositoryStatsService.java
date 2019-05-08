package org.geogig.server.stats.repository;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.events.model.RepositoryEvent;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.RepoStats;
import org.geogig.server.stats.StatsEvent;
import org.geogig.server.stats.storage.RepositoryStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RepositoryStatsService {

    private @Autowired RepositoryStatsRepository store;

    private @Autowired ApplicationEventPublisher bus;

    public @EventListener void onCreated(RepositoryEvent.Created event) {
        RepoInfo repository = event.getRepository();
        UUID id = repository.getId();
        RepoStats stats;
        Optional<RepoStats> current = get(repository);
        if (current.isPresent()) {
            stats = current.get();
        } else {
            stats = new RepoStats(id);
            stats.setNumBranches(1);
            stats = store.saveAndFlush(stats);
            log.info("created initial repo stats {}", stats);
            bus.publishEvent(StatsEvent.Created.builder().source(repository).stats(stats).build());
        }
    }

    public @EventListener void onModified(RepositoryEvent.Updated event) {
        RepoInfo repo = event.getRepository();
    }

    public @EventListener void onDeleted(RepositoryEvent.Deleted event) {
        RepoInfo repo = event.getRepository();
        get(repo).ifPresent(stats -> {
            store.deleteById(stats.getId());
            bus.publishEvent(StatsEvent.Deleted.builder().source(repo).stats(stats).build());
        });
    }

    public @EventListener void onForked(RepositoryEvent.Forked event) {

    }

    public Optional<RepoStats> get(@NonNull RepoInfo repo) {
        Optional<RepoStats> stats = store.findById(repo.getId());
        if (!stats.isPresent()) {
            bus.publishEvent(StatsEvent.Missing.builder().source(repo).build());
        }
        return stats;
    }

    public RepoStats save(@NonNull RepoStats stats) {
        stats = store.saveAndFlush(stats);
        return stats;
    }

}
