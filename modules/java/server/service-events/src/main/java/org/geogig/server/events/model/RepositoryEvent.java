package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.RepoInfo;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class RepositoryEvent implements Event {

    private RepoInfo repository;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends RepositoryEvent implements CreatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Updated extends RepositoryEvent implements UpdatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Deleted extends RepositoryEvent implements DeletedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Forked extends RepositoryEvent implements CreatedEvent {
    }
}
