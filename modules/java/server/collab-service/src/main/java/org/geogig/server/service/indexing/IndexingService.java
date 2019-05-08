package org.geogig.server.service.indexing;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.geogig.server.service.async.AsyncTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import lombok.NonNull;

public class IndexingService {
    private @Autowired AsyncTasksService async;

    public @Async CompletableFuture<Void> forkIndexes(@NonNull UUID fromRepo,
            @NonNull UUID toRepo) {
        return null;
    }
}
