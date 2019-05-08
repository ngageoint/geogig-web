package org.geogig.server.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

public @Entity @Data @NoArgsConstructor class Transaction {

    public static enum Status {
        OPEN, COMMITTED, COMMITTING, CONFLICTS_ON_COMMIT, ABORTED;
    }

    private @Id UUID id;

    private @NonNull @Column(nullable = false) UUID userId;

    private @NonNull @Column(nullable = false) UUID repositoryId;

    private @NonNull @Column(nullable = false) UUID createdByUserId;

    private @NonNull @Column(nullable = false) @Enumerated(EnumType.STRING) Status status;

    private @NonNull @Column(nullable = false) OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime terminatedAt;

    private UUID terminatedByUserId;
}
