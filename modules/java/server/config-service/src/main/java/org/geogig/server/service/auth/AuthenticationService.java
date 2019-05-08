package org.geogig.server.service.auth;

import static com.google.common.base.Preconditions.checkState;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

import lombok.NonNull;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.geogig.server.service.auth", lazyInit = true)
@EntityScan("org.geogig.server.service.auth")
@EnableJpaRepositories("org.geogig.server.service.auth")
public @Service("AuthenticationService") class AuthenticationService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String ROLE_USER = "ROLE_USER";

    private @Autowired TestOnlyUserDetailsManager userDetailsManager;

    public Optional<AuthUser> getCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Optional<AuthUser> currentUser = Optional.empty();

        if (null != authentication) {
            String name = authentication.getName();
            if (null != name) {
                currentUser = userDetailsManager.getUserByUsername(name);
            }
        }
        return currentUser;
    }

    /**
     * @throws IllegalStateException if there's no authenticated user
     */
    public AuthUser requireCurrentUser() {
        AuthUser currentUser = getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("There is no current user"));
        return currentUser;
    }

    public AuthUser requireAdmin() {
        AuthUser admin = requireCurrentUser();
        checkState(isAdmin(admin), "User %s is not an admin: %s", admin.getUsername(),
                admin.getRoles());
        return admin;
    }

    public boolean currentUserIsAdmin() {
        return isAdmin(getCurrentUser());
    }

    public boolean isAdmin(Optional<AuthUser> user) {
        return isAdmin(user.orElse(null));
    }

    public boolean isAdmin(@Nullable AuthUser user) {
        return user != null && user.getRoles().contains(ROLE_ADMIN);
    }

    public AuthUser createUser(@NonNull String name) {
        return userDetailsManager.createUser(name, null, ROLE_USER);
    }

    public void changePassword(@NonNull String newPassword) {
        userDetailsManager.changePassword((String) null, newPassword);
    }

    public Optional<AuthUser> getUser(@NonNull UUID userId) {
        return userDetailsManager.getUser(userId);
    }

    public Optional<AuthUser> getUserByName(@NonNull String userName) {
        return userDetailsManager.getUserByUsername(userName);
    }

    public void setPassword(String userName, String newPassword) {
        AuthUser currentUser = requireCurrentUser();
        AuthUser user = getUserByName(userName).orElseThrow(() -> new NoSuchElementException());
        Preconditions.checkArgument(isAdmin(currentUser) || user.getUsername().equals(userName));

        userDetailsManager.setPassword(userName, newPassword);
    }

    public void delete(@NonNull String userName) {
        userDetailsManager.deleteUser(userName);
    }
}
