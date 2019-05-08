package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.PullRequest;

import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PullRequestEvent implements Event {

    private PullRequest request;

    private @Default Optional<AuthUser> caller = Optional.empty();

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Created extends PullRequestEvent implements CreatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Updated extends PullRequestEvent implements UpdatedEvent {
    }

    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    public static class Deleted extends PullRequestEvent implements DeletedEvent {
    }
}
