package org.geogig.server.service.auth;

import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.geogig.server.model.AuthUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public @Repository interface AuthRepository extends CrudRepository<AuthUser, UUID> {

    public Optional<AuthUser> findByUsername(String username);

    public boolean existsByUsername(String username);

    public @Transactional void deleteByUsername(String username);

}
