package org.geogig.server.websockets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.User;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.ForkEvent;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.ServerEvent.EventTypeEnum;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.UserConnectionEvent;
import org.geogig.web.model.events.EventTopics;
import org.geogig.web.model.events.EventTopics.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import lombok.NonNull;

public @Service class WebSocketsPushEventsService extends AbstractPushEventService {

    private @Autowired RepositoryManagementService repos;

    private @Autowired UserService users;

    private ConcurrentMap<String, UserConnectionEvent> sessions = new ConcurrentHashMap<>();

    final @VisibleForTesting void reset() {
        sessions.clear();
    }

    public List<UserConnectionEvent> getSessions() {
        List<UserConnectionEvent> list = new ArrayList<>(sessions.values());
        return list;
    }

    public Set<String> getLoggedInAdmins() {
        return sessions.values().stream().filter(e -> e.getSubject().isSiteAdmin())
                .map(e -> e.getSubject().getIdentity()).collect(Collectors.toSet());
    }

    protected void sendToAdminsAnd(@NonNull Topic topic, @NonNull ServerEvent event,
            Set<String> users) {

        Set<String> to = getLoggedInAdmins();
        to.addAll(users);
        super.sendToUsers(topic, event, to);
    }

    protected void sendToAdminsAnd(@NonNull Topic topic, @NonNull ServerEvent event,
            String... users) {
        Set<String> to = getLoggedInAdmins();
        if (users != null && users.length > 0) {
            to.addAll(Sets.newHashSet(users).stream().filter(s -> !Strings.isNullOrEmpty(s))
                    .collect(Collectors.toList()));
        }
        super.sendToUsers(topic, event, to);
    }

    public @EventListener void pushUserConnectionEvent(
            @NonNull org.geogig.web.model.UserConnectionEvent event) {

        String sessionId = event.getSessionId();
        Preconditions.checkNotNull(sessionId);
        if (event.isConnected()) {
            sessions.put(sessionId, event);
            send(EventTopics.USER_CONNECTED, event);
        } else {
            sessions.remove(sessionId);
            send(EventTopics.USER_DISCONNECTED, event);
        }
    }

    public @EventListener void pushStoreEvent(org.geogig.web.model.StoreEvent event) {
        super.sendToUsers(EventTopics.STORE_EVENTS, event, getLoggedInAdmins());
    }

    public @EventListener void pushUserEvent(org.geogig.web.model.UserEvent event) {
        EventTypeEnum eventType = event.getEventType();
        String caller = event.getCaller() == null ? null : event.getCaller().getIdentity();
        String self = eventType == EventTypeEnum.MODIFIED ? event.getSubject().getIdentity() : null;
        sendToAdminsAnd(EventTopics.USER_EVENTS, event, caller, self);
    }

    public @EventListener void pushTransactionEvent(org.geogig.web.model.TransactionEvent event) {
        TransactionInfo txInfo = event.getSubject();
        String caller = event.getCaller() == null ? null : event.getCaller().getIdentity();
        String createdBy = txInfo.getCreatedBy().getIdentity();
        String repoOwner = txInfo.getRepository().getOwner().getIdentity();

        if (caller != null) {
            super.sendToUser(caller, EventTopics.TRANSACTION_EVENTS, event);
        }
        if (createdBy != null && !createdBy.equals(caller)) {
            super.sendToUser(createdBy, EventTopics.TRANSACTION_EVENTS, event);
        }
        if (repoOwner != null && !(repoOwner.equals(createdBy) || repoOwner.equals(caller))) {
            super.sendToUser(repoOwner, EventTopics.TRANSACTION_EVENTS, event);
        }
    }

    /**
     * Depending on the event type, notifies the following users:
     * <ul>
     * <li>CREATED: admins, owner, caller, and forked from owner if it's a {@link ForkEvent}
     * <li>UPDATED: admins, owner, caller, and forks owners
     * <li>DELETED: admins, owner, caller, and forks owners
     * </ul>
     */
    public @EventListener void pushRepositoryEvent(org.geogig.web.model.RepositoryEvent event) {
        RepositoryInfo repositoryInfo = event.getSubject();

        final String caller = event.getCaller() == null ? null : event.getCaller().getIdentity();
        final String owner = repositoryInfo.getOwner().getIdentity();
        Set<String> notify = Sets.newHashSet(caller, owner);
        if (event instanceof ForkEvent) {
            RepositoryInfo forkedFrom = repositoryInfo.getForkedFrom();
            String parentOwner = forkedFrom.getOwner().getIdentity();
            notify.add(parentOwner);
        }
        if (event.getEventType() != EventTypeEnum.ADDED) {
            Set<RepoInfo> forks = repos.getForksOf(repositoryInfo.getId(), false);
            for (RepoInfo f : forks) {
                UUID ownerId = f.getOwnerId();
                Optional<User> forkOwner = users.get(ownerId);
                if (forkOwner.isPresent()) {
                    notify.add(forkOwner.get().getIdentity());
                }
            }
        }
        sendToAdminsAnd(EventTopics.REPO_EVENTS, event, notify);
    }

    public @EventListener void pushPullRequestEvent(org.geogig.web.model.PullRequestEvent event) {
        PullRequestInfo pr = event.getSubject();
        String owner = pr.getTargetRepo().getOwner().getIdentity();
        String issuer = pr.getSourceRepo().getOwner().getIdentity();

        sendToAdminsAnd(EventTopics.PR_EVENTS, event, owner, issuer);
    }
}
