package org.geogig.server.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public @Entity class User extends AuditedEntity {

    public static enum UserType {
        INDIVIDUAL, ORGANIZATION;
    }

    private @Id UUID id;

    private @NonNull @Column(name = "identity", unique = true, nullable = false) String identity;

    private @NonNull @Column(nullable = false) UserType type;

    private @Column(nullable = false) boolean siteAdmin;

    private @NonNull @Column(nullable = false) UUID defaultStore;

    @ElementCollection(targetClass = UUID.class, fetch = FetchType.EAGER)
    private Set<UUID> additionalStores = new HashSet<>();

    private String avatarUrl;

    private String gravatarId;

    private String company;

    private String fullName;

    private String emailAddress;

    private String location;

}
