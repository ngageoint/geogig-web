package org.geogig.server.service.auth;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

import com.google.common.base.Preconditions;

import lombok.NonNull;

public class TestOnlyUserDetailsManager implements UserDetailsManager {

    private @Autowired AuthRepository store;

    private final PasswordEncoder passwordEncoder;

    public TestOnlyUserDetailsManager(@NonNull PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<AuthUser> getUserByUsername(String username) {
        return store.findByUsername(username);
    }

    public @Override AuthUser loadUserByUsername(String username) throws UsernameNotFoundException {
        return getUserByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("User " + username + " does not exist"));
    }

    public @Override void createUser(UserDetails user) {
        AuthUser newUser = new AuthUser(UUID.randomUUID(), user);
        createUser(newUser);
    }

    public AuthUser createUser(String userName, @Nullable String password, String... roles) {
        Preconditions.checkNotNull(userName);
        Preconditions.checkNotNull(roles);

        UUID id = UUID.randomUUID();
        AuthUser user = new AuthUser(id, userName, roles);
        user.setPassword(password);
        return createUser(user);
    }

    public AuthUser createUser(AuthUser user) {
        if (store.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("User " + user.getUsername() + " already exists");
        }
        String password = user.getPassword();
        if (null == password) {
            password = "";
        }
        user.setPassword(passwordEncoder.encode(password));
        AuthUser created = store.save(user);
        return created;
    }

    public @Override void updateUser(UserDetails user) {
        Preconditions.checkArgument(user instanceof AuthUser);
        AuthUser authUser = (AuthUser) user;
        store.save(authUser);
    }

    public @Override void deleteUser(@NonNull String username) {
        store.deleteByUsername(username);
    }

    public void setPassword(String userName, String newPassword) {
        AuthUser user = loadUserByUsername(userName);
        Preconditions.checkNotNull(user, "Current user does not exist.");

        user.setPassword(passwordEncoder.encode(newPassword));
        updateUser(user);
    }

    public @Override void changePassword(@NonNull String oldPassword, @NonNull String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean userExists(@NonNull String username) {
        return store.existsByUsername(username);
    }

    public Optional<AuthUser> getUser(@NonNull UUID userId) throws NoSuchElementException {
        return store.findById(userId);
    }

}