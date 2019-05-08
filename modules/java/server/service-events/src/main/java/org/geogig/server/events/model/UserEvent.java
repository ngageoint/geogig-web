package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.User;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class UserEvent implements Event {

    private User user;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends UserEvent implements CreatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Updated extends UserEvent implements UpdatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Deleted extends UserEvent implements DeletedEvent {
    }
}
