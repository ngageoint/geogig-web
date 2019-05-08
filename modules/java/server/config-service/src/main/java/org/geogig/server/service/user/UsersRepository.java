package org.geogig.server.service.user;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;

public @Repository @Transactional interface UsersRepository extends CrudRepository<User, UUID> {

    public Optional<User> findByIdentity(@NonNull String name);

}
