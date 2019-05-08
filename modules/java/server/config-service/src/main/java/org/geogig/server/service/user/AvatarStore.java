package org.geogig.server.service.user;

import java.util.Optional;

import org.geogig.server.model.Avatar;
import org.springframework.stereotype.Service;

import lombok.NonNull;

@Service("AvatarStore")
public interface AvatarStore {

    public Optional<Avatar> findById(@NonNull String id);

    public Avatar save(@NonNull Avatar entity);
}
