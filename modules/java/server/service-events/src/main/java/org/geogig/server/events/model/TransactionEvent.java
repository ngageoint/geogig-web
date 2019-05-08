package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.Transaction;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TransactionEvent implements Event {

    private Transaction transaction;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends TransactionEvent implements CreatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Updated extends TransactionEvent implements UpdatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Deleted extends TransactionEvent implements DeletedEvent {
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Committed extends TransactionEvent {
        private boolean success;
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Aborted extends TransactionEvent {
    }
}
