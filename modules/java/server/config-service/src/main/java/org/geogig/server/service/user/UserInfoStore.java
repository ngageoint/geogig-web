package org.geogig.server.service.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.User;
import org.springframework.stereotype.Service;

@Service("UserInfoStore")
public interface UserInfoStore {

    User create(User user);

    <T extends User> T save(T user);

    Iterable<User> findAll();

    void deleteById(UUID id);

    Optional<User> findById(UUID id);

    Optional<User> findByIdentity(String login);

    boolean addFollower(UUID follower, UUID followee);

    boolean removeFollower(UUID follower, UUID followee);

    /**
     * @return all users the given one follows
     */
    List<User> getFollowers(UUID id);

    /**
     * @return all users following the given one
     */
    List<User> getFollowing(UUID id);

}
