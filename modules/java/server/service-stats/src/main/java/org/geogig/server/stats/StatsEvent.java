package org.geogig.server.stats;

import java.util.Optional;

import org.geogig.server.events.model.Event;
import org.geogig.server.model.AuthUser;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class StatsEvent implements Event {

    private Object source;

    private Object stats;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Missing extends StatsEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends StatsEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static @Value class Updated extends StatsEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static @Value class Deleted extends StatsEvent {
    }

}
