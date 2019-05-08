package org.geogig.server.events.model;

import java.util.Optional;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.PullRequestStatus;

import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PullRequestStatusEvent implements Event {

    private PullRequestStatus status;

    private @Default Optional<AuthUser> caller = Optional.empty();

}
