package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.Store;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class StoreEvent implements Event {

    private Store store;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends StoreEvent implements CreatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Updated extends StoreEvent implements UpdatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Deleted extends StoreEvent implements DeletedEvent {
    }
}
