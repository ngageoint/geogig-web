package org.geogig.server.service.user;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;

public @Service class UsersJPAStore implements UserInfoStore {

    private @Autowired UsersRepository repo;

    public @Override User create(@NonNull User user) {
        checkArgument(!repo.existsById(user.getId()), "User %s already exists", user.getId());
        return repo.save(user);
    }

    public @Override Optional<User> findByIdentity(String name) {
        return repo.findByIdentity(name);
    }

    public @Override boolean addFollower(UUID follower, UUID followee) {
        throw new UnsupportedOperationException();
    }

    public @Override boolean removeFollower(UUID follower, UUID followee) {
        throw new UnsupportedOperationException();
    }

    public @Override List<User> getFollowers(UUID id) {
        throw new UnsupportedOperationException();
    }

    public @Override List<User> getFollowing(UUID id) {
        throw new UnsupportedOperationException();
    }

    public @Override <T extends User> T save(T user) {
        return repo.save(user);
    }

    public @Override Iterable<User> findAll() {
        return repo.findAll();
    }

    public @Override void deleteById(UUID id) {
        repo.deleteById(id);
    }

    public @Override Optional<User> findById(UUID id) {
        return repo.findById(id);
    }
}
