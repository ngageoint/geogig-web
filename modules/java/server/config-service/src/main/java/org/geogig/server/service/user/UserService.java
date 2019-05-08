package org.geogig.server.service.user;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.User;
import org.geogig.server.model.User.UserType;
import org.geogig.server.service.auth.AuthenticationService;
import org.geogig.server.service.stores.StoreService;
import org.opengis.referencing.IdentifiedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import lombok.NonNull;

@Service("UserService")
public class UserService {

    private static final UUID NO_USER_ADMIN_ID = UUID
            .fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    private StoreService repoStores;

    @Autowired
    private UserInfoStore store;

    @Autowired
    private AuthenticationService auth;

    public Optional<User> getAuthenticatedUser() {
        Optional<User> userInfo = Optional.empty();
        Optional<AuthUser> currentUser = auth.getCurrentUser();
        if (currentUser.isPresent()) {
            userInfo = store.findById(currentUser.get().getId());
            if (!userInfo.isPresent()) {
                // return a user with no id nor type
                userInfo = toAdmin(currentUser.get());
            }
        }
        return userInfo;
    }

    public Optional<User> toAdmin(@NonNull AuthUser authUser) {
        if (!auth.isAdmin(authUser)) {
            return Optional.empty();
        }
        // return a user with no id nor type
        User i = new User();
        i.setId(NO_USER_ADMIN_ID);
        i.setIdentity(authUser.getUsername());
        i.setType(UserType.INDIVIDUAL);
        i.setSiteAdmin(true);
        Instant now = Instant.now();
        i.setCreatedAt(now);
        i.setUpdatedAt(now);
        return Optional.of(i);
    }

    public User requireAuthenticatedUser() {
        return getAuthenticatedUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public Optional<User> get(UUID id) {
        return store.findById(id);
    }

    public User getOrFail(UUID id) throws NoSuchElementException {
        return store.findById(id).orElseThrow(
                () -> new NoSuchElementException(String.format("User id %s not found", id)));
    }

    public User deleteByName(@NonNull String name) {
        final User user = resolveOrFail(name);
        return delete(user);
    }

    public User deleteById(@NonNull UUID id) {
        final User user = getOrFail(id);
        return delete(user);
    }

    private User delete(@NonNull User user) {
        try {
            store.deleteById(user.getId());
        } finally {
            auth.delete(user.getIdentity());
        }
        return user;
    }

    public Optional<User> getByName(String name) {
        return store.findByIdentity(name);
    }

    public User getByNameOrFail(String name) {
        return store.findByIdentity(name).orElseThrow(
                () -> new NoSuchElementException(String.format("User %s not found", name)));
    }

    public Iterable<User> getAll() {
        return store.findAll();
    }

    public User create(User userInfo) {
        checkNotNull(userInfo);
        auth.requireAdmin();
        {
            // Optional<UserInfo> currentUser = auth.getCurrentUser();
            // if (!currentUser.isPresent()) {
            // return error(HttpStatus.UNAUTHORIZED, "Authentication required");
            // }
            // if (!auth.isAdmin(currentUser)) {
            // return error(HttpStatus.FORBIDDEN, "Authenticated user is not an administrator");
            // }
        }
        checkArgument(null == userInfo.getId(),
                "User id must not be present for a create user request: %s", userInfo.getId());

        final String userName = userInfo.getIdentity();
        checkArgument(!Strings.isNullOrEmpty(userName), "User name not provided in identity");
        checkArgument(null != userInfo.getType(), "User type not provided, must be one of %s",
                Arrays.toString(User.UserType.values()));

        // final UserInfoPrivateProfile privateProfile = userInfo.getPrivateProfile();
        checkPrivateProfile(userInfo);

        Optional<AuthUser> authUserOp = auth.getUserByName(userName);
        final AuthUser authUser;
        if (authUserOp.isPresent()) {
            authUser = authUserOp.get();
            checkArgument(!this.getByName(userName).isPresent(), "User name %s is not available",
                    userName);
        } else {
            authUser = auth.createUser(userName);
        }

        if (auth.isAdmin(authUser) != userInfo.isSiteAdmin()) {
            // REVISIT: change authUser role?
        }

        userInfo.setId(authUser.getId());

        userInfo = store.create(userInfo);
        return userInfo;
    }

    private void checkPrivateProfile(User privateProfile) {
        checkArgument(null != privateProfile,
                "No private profile provided, it should at least contain the default repository store");
        checkArgument(null != privateProfile.getDefaultStore(),
                "No default repository store provided");

        verifyStore(privateProfile.getDefaultStore());
        Set<UUID> additionalStores = privateProfile.getAdditionalStores();
        if (null != additionalStores) {
            for (UUID store : additionalStores) {
                verifyStore(store);
            }
        }
    }

    /**
     * Either {@link IdentifiedObject#getIdentity() name} or {@link IdentifiedObject#getId() id}
     * could be provided, id is checked first.
     * 
     * @param storeIdentifier
     */
    private void verifyStore(UUID storeId) {
        checkArgument(storeId != null, "repository store id shall be provided");

        repoStores.get(storeId).orElseThrow(() -> new IllegalArgumentException(
                String.format("Repository store with id %s does not exist", storeId)));

    }

    public User modify(User userInfo) {
        // boolean allowed = true;
        // {
        // final UserInfo currentUser = auth.getCurrentUser().orElse(null);
        //
        // if (null == currentUser) {
        // // return error(HttpStatus.UNAUTHORIZED, "Authentication required");
        // }
        // allowed = true;// auth.isAdmin(currentUser);
        // }

        checkArgument(null != userInfo.getId(), "user id must be provided");
        checkArgument(!Strings.isNullOrEmpty(userInfo.getIdentity()),
                "User name not provided in identity");
        checkArgument(null != userInfo.getType(), "User type not provided, must be one of %s",
                Arrays.toString(User.UserType.values()));

        final User oldUserInfo = resolveOrFail(userInfo.getId());

        if (!oldUserInfo.getIdentity().equals(userInfo.getIdentity())) {
            Optional<User> nameMatch = getByName(userInfo.getIdentity());
            if (nameMatch.isPresent() && !userInfo.getId().equals(nameMatch.get().getId())) {
                throw new IllegalArgumentException(
                        "tried to rename user but identity '%s' is already taken");
            }
        }
        // allowed |= oldUserInfo.getId().equals(userInfo.getId());
        // if (!allowed) {
        // return error(HttpStatus.FORBIDDEN,
        // "Authenticated user is not the one being updated nor administrator");
        // }
        checkPrivateProfile(userInfo);
        userInfo = store.save(userInfo);
        return userInfo;
    }

    /**
     * @return {@code true} if {@code user} is a new follower of {@code followee} when this method
     * 
     *         returns, {@code false} if it already was
     */
    public boolean follow(String userIdentity, String followeeIdentity) {
        User user = resolveOrFail(userIdentity);
        User followee = resolveOrFail(followeeIdentity);

        return store.addFollower(user.getId(), followee.getId());
    }

    public boolean unfollow(String userIdentity, String followeeIdentity) {
        User user = resolveOrFail(userIdentity);
        User followee = resolveOrFail(followeeIdentity);
        boolean removed = store.removeFollower(user.getId(), followee.getId());
        return removed;
    }

    public List<User> getFollowers(String userIdentity) {
        User user = resolveOrFail(userIdentity);
        return store.getFollowers(user.getId());
    }

    public List<User> getFollowing(String userIdentity) {
        User user = resolveOrFail(userIdentity);
        return store.getFollowing(user.getId());
    }

    public User resolveOrFail(UUID userId) {
        return get(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    public User resolveOrFail(String userIdentity) {
        return getByName(userIdentity)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userIdentity));
    }
}
