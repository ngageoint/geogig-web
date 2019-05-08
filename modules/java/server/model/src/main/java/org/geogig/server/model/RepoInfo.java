package org.geogig.server.model;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "ownerId", "identity" }))
public @Entity class RepoInfo extends AuditedEntity {

    private @Id UUID id;

    private @Column(name = "identity", nullable = false) String identity;

    private boolean deleted;

    private @Column(nullable = false) UUID storeId;

    private @Column(nullable = false) UUID ownerId;

    private @Column(nullable = true) UUID forkedFrom;

    private @Embedded @Column(nullable = true) List<String> originSnapshot;

    private int issueCounter;

    private String description;

    private boolean enabled;
}
