package org.geogig.server.websockets;

import java.util.Collections;
import java.util.List;

import org.geogig.web.model.ActivityLogRequest;
import org.geogig.web.model.ActivityLogResponse;
import org.geogig.web.model.UserConnectionEvent;
import org.geogig.web.model.events.GetActiveUsersQuery;
import org.geogig.web.model.events.GetActiveUsersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import lombok.NonNull;

public @Controller class WebSocketsRPCController {

    private @Autowired WebSocketsPushEventsService userEvents;

    private @Autowired ActivityLogService logService;

    @MessageMapping("/rpc/activeusers")
    @SendToUser(destinations = "/rpc/activeusers", broadcast = false)
    public GetActiveUsersResponse getActiveUsers(@NonNull @Payload GetActiveUsersQuery query) {
        List<UserConnectionEvent> list = userEvents.getSessions();
        // revisit: this doesn't update the UserInfo's, might have changed
        Collections.sort(list, (o, n) -> o.getTimestamp().compareTo(n.getTimestamp()));
        return new GetActiveUsersResponse(list);
    }

    @MessageMapping("/rpc/activity")
    @SendToUser(destinations = "/rpc/activity", broadcast = false)
    public ActivityLogResponse getActivity(@NonNull @Payload ActivityLogRequest request) {
        return logService.getActivity(request);
    }

}
