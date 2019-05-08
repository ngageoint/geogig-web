package org.geogig.web.client;

import java.util.UUID;
import java.util.function.Supplier;

import lombok.NonNull;

public abstract class Identified<T> {

    private T info;

    protected final Client client;

    private final Supplier<UUID> id;

    Identified(@NonNull Client client, @NonNull T info, @NonNull Supplier<UUID> id) {
        this.client = client;
        this.info = info;
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    protected void updateInfo(T info) {
        this.info = info;
    }

    public UUID getId() {
        return id.get();
    }

    public T getInfo() {
        return info;
    }

    public @Override String toString() {
        return info.toString();
    }
}
