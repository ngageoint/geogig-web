package org.geogig.server.service.async;


import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import lombok.NonNull;

public interface Job<T> {

    public CompletableFuture<T> run(ApplicationContext context);

    public Optional<UUID> getTransaction();

    public default String getDescription() {
        return getClass().getSimpleName();
    }

    public default boolean isAutoPrune() {
        return false;
    }

    public default @Nullable AsyncTaskProgress getProgressListener() {
        return null;
    }

    public @NonNull UUID getCallerUser();
}
