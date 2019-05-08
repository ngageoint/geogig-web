package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import java.util.function.Supplier;

public class Identity<T> extends Identified<T> {

    private Supplier<String> identity;

    Identity(Client client, T info, Supplier<UUID> id, Supplier<String> identity) {
        super(client, info, id);
        checkNotNull(identity);
        this.identity = identity;
    }

    public String getIdentity() {
        return identity.get();
    }
}
