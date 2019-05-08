package org.geogig.web.model.events;

import java.time.Instant;

import lombok.Data;

public @Data class GetActiveUsersQuery {

    private Instant since, until;
}
