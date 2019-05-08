package org.geogig.server.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;
import lombok.NonNull;

@Entity
@Table(indexes = { @Index(columnList = "username", unique = true) })
public @Data class AuthUser implements UserDetails {
    private static final long serialVersionUID = 1L;

    private @Id @NonNull UUID id;

    private @NonNull @Column(nullable = false) String username;

    private String password;

    private boolean enabled = true;

    private boolean credentialsNonExpired = true;

    private boolean accountNonLocked = true;

    private boolean accountNonExpired = true;

    private @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER) Set<String> roles = new HashSet<>();

    AuthUser() {
        //
    }

    public AuthUser(UUID id, String name, String... roles) {
        this.id = id;
        this.username = name;
        if (roles != null) {
            this.roles = roles == null || roles.length == 0 ? Collections.emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(Arrays.asList(roles)));
        }
    }

    public AuthUser(UUID id, UserDetails prototype) {
        this.id = id;
        this.password = prototype.getPassword();
        roles = new HashSet<>();
        roles = prototype.getAuthorities().stream().map((g) -> g.getAuthority())
                .collect(Collectors.toSet());
        username = prototype.getUsername();
        accountNonExpired = prototype.isAccountNonExpired();
        accountNonLocked = prototype.isAccountNonLocked();
        credentialsNonExpired = prototype.isCredentialsNonExpired();
        enabled = prototype.isEnabled();
    }

    public UUID getId() {
        return id;
    }

    public @Override Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map((role) -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }

}