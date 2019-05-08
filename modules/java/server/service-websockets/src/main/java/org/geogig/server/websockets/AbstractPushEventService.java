package org.geogig.server.websockets;

import java.util.Set;
import java.util.stream.Collectors;

import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.events.EventTopics.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract class AbstractPushEventService {

    protected @Autowired ActivityLogService logService;

    protected @Autowired SimpMessagingTemplate template;

    protected @Autowired PresentationService presentation;

    protected @Autowired UserService users;

    protected void send(@NonNull Topic topic, @NonNull ServerEvent payload) {
        Preconditions.checkArgument(topic.eventType().isInstance(payload));
        send(topic.path(), payload);
    }

    protected void sendToUsers(@NonNull Topic topic, @NonNull ServerEvent event,
            @NonNull String... users) {

        Set<String> userSet = Sets.newHashSet(users).stream()
                .filter(user -> !Strings.isNullOrEmpty(user)).collect(Collectors.toSet());

        sendToUsers(topic, event, userSet);
    }

    protected void sendToUsers(@NonNull Topic topic, @NonNull ServerEvent event,
            @NonNull Set<String> users) {

        users = users.stream().filter(s -> !Strings.isNullOrEmpty(s)).collect(Collectors.toSet());
        users.forEach(user -> sendToUser(user, topic, event));
    }

    protected void sendToUser(@NonNull String user, @NonNull Topic topic,
            @NonNull ServerEvent event) {
        Preconditions.checkArgument(topic.eventType().isInstance(event));
        String destination = topic.path();
        if (log.isTraceEnabled()) {
            log.trace("Sending push event to user {}, topic {}: {}", user, destination, event);
        } else if (log.isDebugEnabled()) {
            log.debug("Sending push event to user {}, topic {}", user, destination,
                    event.getObjectType());
        }
        try {
            template.convertAndSendToUser(user, destination, event);
        } catch (MessagingException me) {
            me.printStackTrace();
        }
        // System.err.printf("Sent to %s: %s %s %s (%s)\n", user, event.getEventType(),
        // event.getClass().getSimpleName(), LocalTime.now(),
        // Thread.currentThread().getName());
    }

    protected void send(@NonNull String topic, @NonNull ServerEvent payload) {
        if (log.isTraceEnabled()) {
            log.trace("Sending push event {} to topic {}: {}", payload.getObjectType(), topic,
                    payload);
        } else {
            log.debug("Sending push event {} to topic {}", payload.getObjectType(), topic);
        }
        template.convertAndSend(topic, payload);
    }
}
