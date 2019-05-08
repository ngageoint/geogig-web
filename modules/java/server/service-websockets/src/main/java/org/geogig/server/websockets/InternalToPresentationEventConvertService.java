package org.geogig.server.websockets;

import java.security.Principal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geogig.server.events.model.Event;
import org.geogig.server.events.model.RepositoryEvent.Forked;
import org.geogig.server.model.AuthUser;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.PullRequestEvent;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.PullRequestStatusEvent;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.ServerEvent.EventTypeEnum;
import org.geogig.web.model.StoreEvent;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.TransactionEvent;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.UserConnectionEvent;
import org.geogig.web.model.UserEvent;
import org.geogig.web.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.google.common.base.Preconditions;

import lombok.NonNull;

/**
 * A service that converts internal {@link Event org.geogig.server.events.model.Event}s to their
 * public API representation {@link ServerEvent org.geogig.web.model.ServerEvent} and re-publish
 * them to the application's {@link ApplicationEventPublisher} to be either pushed up as real time
 * events, saved to an event log, etc.
 *
 */
public @Service class InternalToPresentationEventConvertService extends AbstractPushEventService {

    private @Autowired UserService users;

    @SuppressWarnings("unchecked")
    private <T extends ServerEvent> T setId(T event) {
        return (T) event.eventId(UUID.randomUUID());
    }

    public @EventListener UserConnectionEvent onUserConnected(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        if (sha.isHeartbeat()) {
            return null;
        }

        Preconditions.checkNotNull(sha.getSessionId());
        Optional<User> user = getUserFromSessionevent(event);
        org.geogig.web.model.UserConnectionEvent payload = null;
        if (user.isPresent()) {
            final @Nullable UserInfo userInfo = presentation.toInfo(user.get());
            OffsetDateTime timestamp = OffsetDateTime
                    .ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault());
            String host = getHost(sha);
            String sessionId = sha.getSessionId();
            payload = (UserConnectionEvent) new UserConnectionEvent().connected(true)
                    .sessionId(sessionId).ipAddress(host).subject(userInfo)
                    .eventType(EventTypeEnum.ADDED).timestamp(timestamp);
        }
        return setId(payload);
    }

    public @EventListener UserConnectionEvent onUserDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        Preconditions.checkNotNull(sha.getSessionId());

        Optional<User> user = getUserFromSessionevent(event);
        if (user.isPresent()) {
            final @Nullable UserInfo userInfo = presentation.toInfo(user.get());
            OffsetDateTime timestamp = OffsetDateTime
                    .ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault());
            String host = getHost(sha);
            String sessionId = sha.getSessionId();
            UserConnectionEvent payload = (UserConnectionEvent) new UserConnectionEvent()
                    .connected(false).sessionId(sessionId).ipAddress(host).subject(userInfo)
                    .eventType(EventTypeEnum.DELETED).timestamp(timestamp);
            return setId(payload);
        }
        return null;
    }

    private Optional<User> getUserFromSessionevent(@NonNull AbstractSubProtocolEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            final String login = principal.getName();
            Optional<User> user;
            try {
                user = users.getByName(login);
            } catch (Exception e) {
                return Optional.empty();
            }
            if (user.isPresent()) {
                return user;
            }
            if (principal instanceof UsernamePasswordAuthenticationToken) {
                Object auth = UsernamePasswordAuthenticationToken.class.cast(principal)
                        .getPrincipal();
                if (auth instanceof AuthUser) {
                    AuthUser au = AuthUser.class.cast(auth);
                    return users.toAdmin(au);
                }
            }
        }
        return Optional.empty();
    }

    private String getHost(StompHeaderAccessor sha) {
        GenericMessage<?> gm = (GenericMessage<?>) sha.getHeader("simpConnectMessage");
        String host = null;
        if (gm != null) {
            MessageHeaders headers = gm.getHeaders();
            @SuppressWarnings("unchecked")
            Map<String, String> customHeaders = (Map<String, String>) headers
                    .get("simpSessionAttributes");
            host = customHeaders.get("client-ip-address");
        }
        return host;
    }

    private EventTypeEnum getEventType(Event serverEvent) {
        if (serverEvent instanceof Event.CreatedEvent) {
            return EventTypeEnum.ADDED;
        }
        if (serverEvent instanceof Event.UpdatedEvent) {
            return EventTypeEnum.MODIFIED;
        }
        if (serverEvent instanceof Event.DeletedEvent) {
            return EventTypeEnum.DELETED;
        }
        return null;
    }

    public @EventListener org.geogig.web.model.StoreEvent transformAndPublishStoreEvent(
            org.geogig.server.events.model.StoreEvent event) {
        Store store = event.getStore();
        StoreInfo storeInfo = presentation.toInfo(store);

        final EventTypeEnum eventType = getEventType(event);
        Instant timestamp;
        if (eventType == EventTypeEnum.ADDED) {
            timestamp = store.getCreatedAt();
        } else {
            if (eventType == EventTypeEnum.MODIFIED) {
                timestamp = store.getUpdatedAt();
            } else {
                timestamp = Instant.now();
            }
        }
        OffsetDateTime ts = OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        ServerEvent payload = new org.geogig.web.model.StoreEvent().subject(storeInfo)
                .eventType(eventType).timestamp(ts).caller(getCaller(event).orElse(null));

        return setId((StoreEvent) payload);
    }

    public @EventListener org.geogig.web.model.UserEvent transformAndPublishUserEvent(
            org.geogig.server.events.model.UserEvent event) {
        User user = event.getUser();
        UserInfo userInfo = presentation.toInfo(user);

        final EventTypeEnum eventType = getEventType(event);
        Instant timestamp;
        if (eventType == EventTypeEnum.ADDED) {
            timestamp = user.getCreatedAt();
        } else {
            if (eventType == EventTypeEnum.MODIFIED) {
                timestamp = user.getUpdatedAt();
            } else {
                timestamp = Instant.now();
            }
        }
        OffsetDateTime ts = OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        ServerEvent payload = new org.geogig.web.model.UserEvent().subject(userInfo)
                .eventType(eventType).timestamp(ts).caller(getCaller(event).orElse(null));

        return setId((UserEvent) payload);
    }

    public @EventListener org.geogig.web.model.RepositoryEvent transformAndPublishRepositoryEvent(
            org.geogig.server.events.model.RepositoryEvent event) {

        final RepoInfo repo = event.getRepository();
        org.geogig.web.model.RepositoryEvent repositoryEvent;
        if (event instanceof Forked) {
            repositoryEvent = new org.geogig.web.model.ForkEvent();
        } else {
            repositoryEvent = new org.geogig.web.model.RepositoryEvent();
        }
        RepositoryInfo repoInfo = presentation.toInfo(repo);
        final EventTypeEnum eventType = getEventType(event);
        Instant timestamp;
        if (eventType == EventTypeEnum.ADDED) {
            timestamp = repo.getCreatedAt();
        } else {
            if (eventType == EventTypeEnum.MODIFIED) {
                timestamp = repo.getUpdatedAt();
            } else {
                timestamp = Instant.now();
            }
        }

        OffsetDateTime ts = OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        repositoryEvent.subject(repoInfo).eventType(eventType).timestamp(ts)
                .caller(getCaller(event).orElse(null));
        return setId(repositoryEvent);
    }

    public @EventListener org.geogig.web.model.TransactionEvent transformAndPublishTransactionEvent(
            org.geogig.server.events.model.TransactionEvent event) {

        final Transaction tx = event.getTransaction();
        final TransactionInfo txInfo = presentation.toInfo(tx);

        TransactionEvent apiEvent = new org.geogig.web.model.TransactionEvent();
        apiEvent.subject(txInfo);
        apiEvent.caller(getCaller(event).orElse(null));

        final EventTypeEnum eventType = getEventType(event);
        OffsetDateTime timestamp;
        if (eventType == EventTypeEnum.ADDED) {
            timestamp = tx.getCreatedAt();
        } else {
            if (eventType == EventTypeEnum.MODIFIED) {
                timestamp = tx.getUpdatedAt();
            } else {
                timestamp = OffsetDateTime.now();
            }
        }
        apiEvent.setEventType(eventType);
        apiEvent.setTimestamp(timestamp);
        return setId(apiEvent);
    }

    public @EventListener org.geogig.web.model.PullRequestEvent transformAndPublishPullRequestEvent(
            org.geogig.server.events.model.PullRequestEvent event) {

        PullRequestInfo prInfo = presentation.toInfo(event.getRequest());
        final EventTypeEnum eventType = getEventType(event);
        OffsetDateTime timestamp;
        if (eventType == EventTypeEnum.ADDED) {
            timestamp = prInfo.getCreatedAt();
        } else {
            if (eventType == EventTypeEnum.MODIFIED) {
                timestamp = prInfo.getUpdatedAt();
            } else {
                timestamp = OffsetDateTime.now();
            }
        }

        org.geogig.web.model.PullRequestEvent prEvent = new PullRequestEvent();
        prEvent.subject(prInfo).eventType(eventType).timestamp(timestamp)
                .caller(getCaller(event).orElse(null));

        return setId(prEvent);
    }

    public @EventListener org.geogig.web.model.PullRequestStatusEvent transformAndPublishPullRequestStatusEvent(
            org.geogig.server.events.model.PullRequestStatusEvent event) {

        PullRequestInfo prInfo = presentation.toInfo(event.getStatus().getRequest());
        PullRequestStatus prStatusInfo = presentation.toInfo(event.getStatus());

        final EventTypeEnum eventType = EventTypeEnum.MODIFIED;
        OffsetDateTime timestamp = OffsetDateTime.now();

        org.geogig.web.model.PullRequestStatusEvent prStatusEvent = new PullRequestStatusEvent();
        prStatusEvent.status(prStatusInfo).subject(prInfo).eventType(eventType).timestamp(timestamp)
                .caller(getCaller(event).orElse(null));

        return setId(prStatusEvent);
    }

    private Optional<UserInfo> getCaller(org.geogig.server.events.model.Event event) {
        return event.getCaller().flatMap(this::getCaller);
    }

    private Optional<UserInfo> getCaller(AuthUser authUser) {
        Optional<User> user = users.get(authUser.getId()).or(() -> users.toAdmin(authUser));
        return user.map(presentation::toInfo);
    }
}
