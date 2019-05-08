package org.geogig.web.wsclient;

import java.lang.reflect.Type;

import org.geogig.web.model.ServerEvent;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import com.google.common.eventbus.EventBus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EventPublisherStompFrameHandler implements StompFrameHandler {

    private final @NonNull EventBus clientEventBus;

    private final @NonNull String sessionId;

    /**
     * @return the generic {@link ServerEvent ServerEvent.class}, the JSON mapper takes care of
     *         converting to the required concrete type
     */
    public @Override Type getPayloadType(StompHeaders headers) {
        return ServerEvent.class;
    }

    public @Override void handleFrame(StompHeaders headers, Object payload) {
        ServerEvent event = (ServerEvent) payload;
        if (log.isTraceEnabled()) {
            log.trace("STOMP event received: {}", event);
        } else if (log.isDebugEnabled()) {
            log.debug("STOMP event received: {} {}:{}. Session {}. Time: {}, triggered by: {}",
                    event.getEventId(), //
                    event.getClass().getSimpleName(), //
                    event.getEventType(), //
                    sessionId, //
                    event.getTimestamp(), //
                    (event.getCaller() == null ? null : event.getCaller().getIdentity()));
        }
        clientEventBus.post(event);
    }

}
