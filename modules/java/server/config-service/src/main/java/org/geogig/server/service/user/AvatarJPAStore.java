package org.geogig.server.service.user;

import java.util.Optional;

import org.geogig.server.model.Avatar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public @Service class AvatarJPAStore implements AvatarStore {

    private @Autowired AvatarRepository repo;

    public @Override Optional<Avatar> findById(String id) {
        return repo.findById(id);
    }

    public @Override Avatar save(Avatar entity) {
        return repo.save(entity);
    }
}
