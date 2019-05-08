package org.geogig.server.service.user;

import org.geogig.server.model.Avatar;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public @Repository interface AvatarRepository extends CrudRepository<Avatar, String> {

}
