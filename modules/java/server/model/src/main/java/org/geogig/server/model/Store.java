package org.geogig.server.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

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
public @Entity class Store extends AuditedEntity {

    private @Id UUID id;

    private @Column(name = "identity", unique = true, nullable = false) String identity;

    private String description;

    private boolean enabled;

    private @Column(nullable = false) String baseURI;
}
