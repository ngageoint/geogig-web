package org.geogig.server.app.gateway;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.geogig.server.model.AuthUser;
import org.geogig.server.model.User;
import org.geogig.server.service.auth.AuthenticationService;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.user.UserAvatarService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.UserInfo;
import org.geogig.web.server.api.UsersApi;
import org.geogig.web.server.api.UsersApiDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link UsersApi}, handles the
 * REST request/response/error handling aspects of the API, and delegates business logic to a
 * {@link UserService}.
 */
@Slf4j
public @Service class UsersServiceApi extends AbstractService implements UsersApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired UserService users;

    private @Autowired UserAvatarService userAvatars;

    private @Autowired AuthenticationService auth;

    private @Autowired RepositoryManagementService repos;

    public @Override ResponseEntity<UserInfo> createUser(UserInfo userInfo) {
        User user = presentation.toModel(userInfo);
        return super.create(() -> presentation.toInfo(users.create(user)));
    }

    public @Override ResponseEntity<Void> resetPassword(String userName, String newPassword) {
        Optional<AuthUser> authenticatedUser = auth.getCurrentUser();
        if (!authenticatedUser.isPresent()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Optional<AuthUser> authUser = auth.getUserByName(userName);
        if (!authUser.isPresent()) {
            return super.notFound("User %s does not exist", userName);
        }

        final boolean sameUser = authenticatedUser.get().getUsername().equals(userName);
        if (auth.isAdmin(authenticatedUser) || sameUser) {
            auth.setPassword(userName, newPassword);
            if (sameUser) {
                getRequest()
                        .ifPresent((r) -> new SecurityContextLogoutHandler().logout(r, null, null));
                log.debug("User %s automatically logged out after changing his password", userName);
            }
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    public @Override ResponseEntity<Void> deleteUser(String user) {
        return super.run(HttpStatus.NO_CONTENT, ((Runnable) () -> users.deleteByName(user)));
    }

    public @Override ResponseEntity<UserInfo> modifyUser(UserInfo userInfo) {
        User user = presentation.toModel(userInfo);
        return super.run(HttpStatus.OK, () -> presentation.toInfo(users.modify(user)));
    }

    public @Override ResponseEntity<List<RepositoryInfo>> getUserRepositories(String user) {
        return super.ok(() -> Lists.transform(repos.getByUser(user), presentation::toInfo));
    }

    public @Override ResponseEntity<UserInfo> getSelf() {
        Optional<User> self = users.getAuthenticatedUser();
        if (self.isPresent()) {
            return ok(presentation.toInfo(self.get()));
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    }

    public @Override ResponseEntity<UserInfo> getUser(String user) {
        Optional<User> self = users.getAuthenticatedUser();
        if (self.isPresent() && self.get().getIdentity().equals(user)) {
            return super.ok(presentation.toInfo(self.get()));
        }
        return super.okOrNotFound(users.getByName(user).map(presentation::toInfo));
    }

    public @Override ResponseEntity<byte[]> getUserAvatar(@NonNull String user) {
        Optional<User> requestedUser = users.getByName(user);
        if (requestedUser.isPresent()) {
            String emailAddress = requestedUser.get().getEmailAddress();
            if (emailAddress != null) {
                return getAvatar(emailAddress);
            }
        }
        return super.notFound();
    }

    public @Override ResponseEntity<byte[]> getAvatar(@NonNull String email) {
        CompletableFuture<byte[]> rawAvatar = userAvatars.getRawByEmailAdress(email);
        try {
            byte[] bs = rawAvatar.get();
            return super.ok(bs);
        } catch (InterruptedException | ExecutionException e) {
            log.trace("Error getting avatar for {}", email, e);
            return super.notFound();
        }
    }

    public @Override ResponseEntity<List<UserInfo>> getUsers() {
        return super.ok(
                Lists.newArrayList(Iterables.transform(users.getAll(), presentation::toInfo)));
    }

    public @Override ResponseEntity<Boolean> follow(String user, String followee) {
        return run(HttpStatus.OK, () -> users.follow(user, followee));
    }

    public @Override ResponseEntity<Boolean> unfollow(String user, String followee) {
        return run(HttpStatus.OK, () -> users.unfollow(user, followee));
    }

    public @Override ResponseEntity<List<UserInfo>> listFollowers(String user) {
        return super.ok(() -> Lists.transform(users.getFollowers(user), presentation::toInfo));
    }

    public @Override ResponseEntity<List<UserInfo>> listFollowing(String user) {
        return super.ok(() -> Lists.transform(users.getFollowing(user), presentation::toInfo));
    }

}
