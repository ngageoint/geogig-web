package org.geogig.web.model.events;

import java.util.List;

import org.geogig.web.model.UserConnectionEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public @Data @AllArgsConstructor @NoArgsConstructor class GetActiveUsersResponse {

    private List<UserConnectionEvent> users;
}
