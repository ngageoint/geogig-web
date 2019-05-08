package org.geogig.server.service.rpc;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Job;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.springframework.context.ApplicationContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class SimpleCommandJob<T, R> implements Job<R> {

    private final @NonNull AbstractGeoGigOp<T> command;

    private final Transaction transaction;

    private final @NonNull User issuer;

    private final @NonNull Function<T, R> resultMapper;

    public @Override UUID getCallerUser() {
        return issuer.getId();
    }

    public @Override String getDescription() {
        return command.getClass().getSimpleName();
    }

    public @Override Optional<UUID> getTransaction() {
        return Optional.ofNullable(transaction).map(t -> t.getId());
    }

    public @Override CompletableFuture<R> run(ApplicationContext context) {
        try {
            T result = command.call();
            R ret = resultMapper.apply(result);
            return CompletableFuture.completedFuture(ret);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
