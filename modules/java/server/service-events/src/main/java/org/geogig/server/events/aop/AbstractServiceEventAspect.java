package org.geogig.server.events.aop;

import java.util.Optional;

import org.geogig.server.events.model.Event;
import org.geogig.server.model.AuthUser;
import org.geogig.server.service.auth.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractServiceEventAspect {

    private @Autowired ApplicationEventPublisher bus;

    protected @Autowired AuthenticationService auth;

    protected void publishEvent(@NonNull Event event) {
        if (!event.getCaller().isPresent()) {
            Optional<AuthUser> caller = auth.getCurrentUser();
            event.setCaller(caller);
        }
        bus.publishEvent(event);
        log.debug("Issued event {}", event);
    }
}
