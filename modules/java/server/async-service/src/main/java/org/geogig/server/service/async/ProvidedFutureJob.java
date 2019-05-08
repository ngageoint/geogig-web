package org.geogig.server.service.async;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;

import lombok.NonNull;
import lombok.Setter;

public class ProvidedFutureJob<T> implements Job<T> {

    private CompletableFuture<T> future;

    private UUID callerUser;

    private @Setter UUID transaction;

    private @Setter String description;

    private Supplier<AsyncTaskProgress> progressSupplier = () -> null;

    public ProvidedFutureJob(@NonNull CompletableFuture<T> future, @NonNull UUID callerUser) {
        this.future = future;
        this.callerUser = callerUser;
    }

    public void setProgressSupplier(@NonNull Supplier<AsyncTaskProgress> supplier) {
        this.progressSupplier = supplier;
    }

    public @Override AsyncTaskProgress getProgressListener() {
        return progressSupplier.get();
    }

    public @Override CompletableFuture<T> run(ApplicationContext context) {
        return future;
    }

    public @Override Optional<UUID> getTransaction() {
        return Optional.ofNullable(transaction);
    }

    public @Override String getDescription() {
        return description;
    }

    public @Override @NonNull UUID getCallerUser() {
        return callerUser;
    }
}
