package org.geogig.server.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Wither;

@Data
@EqualsAndHashCode(callSuper = true)
@Wither
@NoArgsConstructor
@AllArgsConstructor
@Table(//
        uniqueConstraints = @UniqueConstraint(columnNames = { "id", "repositoryId" }), //
        indexes = { @Index(columnList = "repositoryId", unique = false) })
public @Entity class PullRequest extends AuditedEntity {

    public static enum Status {
        OPEN, CLOSED, MERGED
    }

    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private @Id Integer id;

    private @Column(nullable = false) UUID repositoryId;

    private @NonNull @Column(nullable = false) String targetBranch;

    private @NonNull @Column(nullable = false) String title;

    private String description;

    private @NonNull @Enumerated(EnumType.ORDINAL) @Column(nullable = false) Status status;

    private @NonNull @Column(nullable = false) UUID issuerUser;

    private @NonNull @Column(nullable = false) UUID issuerRepo;

    private @NonNull @Column(nullable = false) String issuerBranch;

    private UUID closedByUserId;

    public boolean isClosed() {
        return status.equals(Status.CLOSED);
    }

    public boolean isMerged() {
        return status.equals(Status.MERGED);
    }

    public boolean isOpen() {
        return status.equals(Status.OPEN);
    }
}
