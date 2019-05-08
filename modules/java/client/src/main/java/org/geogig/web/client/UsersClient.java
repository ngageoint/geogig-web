package org.geogig.web.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.UsersApi;
import org.geogig.web.model.UserInfo;

import lombok.NonNull;

public class UsersClient extends AbstractServiceClient<UsersApi> {

    UsersClient(Client client) {
        super(client, client.users);
    }

    public User create(UserInfo info) {
        UserInfo createdUser;
        try {
            createdUser = api.createUser(info);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new User(client, createdUser);
    }

    public User modify(UserInfo info) {
        UserInfo modifiedUser;
        try {
            modifiedUser = api.modifyUser(info);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new User(client, modifiedUser);
    }

    public boolean delete(String userName) {
        try {
            api.deleteUser(userName);
            return true;
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return false;
        }
    }

    public List<User> getUsers() {
        List<UserInfo> infos;
        try {
            infos = api.getUsers();
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        List<User> clients = infos.stream().map((u) -> new User(client, u))
                .collect(Collectors.toList());
        return clients;
    }

    public User getSelf() {
        UserInfo user;
        try {
            user = api.getSelf();
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new User(client, user);
    }

    public User get(@NonNull String name) {
        try {
            UserInfo user = api.getUser(name);
            return new User(client, user);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public Optional<User> tryGet(String name) {
        try {
            return Optional.of(get(name));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public Optional<BufferedImage> getAvatarByUser(@NonNull String userName) {
        Optional<BufferedImage> avatar = Optional.empty();
        try {
            byte[] raw = api.getUserAvatar(userName);
            if (raw != null && raw.length > 0) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
                avatar = Optional.of(img);
            }
        } catch (Exception ignore) {
//            log.debug("Error loading avatar for {}", userName, ignore);
        }
        return avatar;
    }

    public Optional<BufferedImage> getAvatarByEmail(@NonNull String email) {
        Optional<BufferedImage> avatar = Optional.empty();
        try {
            byte[] raw = api.getAvatar(email);
            if (raw != null && raw.length > 0) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
                avatar = Optional.of(img);
            }
        } catch (Exception ignore) {
//            log.debug("Error loading avatar for {}", email, ignore);
        }
        return avatar;
    }
}
