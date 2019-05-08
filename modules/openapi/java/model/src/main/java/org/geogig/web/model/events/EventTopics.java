package org.geogig.web.model.events;

import org.geogig.web.model.PullRequestEvent;
import org.geogig.web.model.RepositoryEvent;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.StoreEvent;
import org.geogig.web.model.TransactionEvent;
import org.geogig.web.model.UserConnectionEvent;
import org.geogig.web.model.UserEvent;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

public @UtilityClass class EventTopics {

    public @Value @Accessors(fluent = true) static class Topic {
        private final boolean broadcast;

        private final @NonNull String path;

        private final @NonNull Class<? extends ServerEvent> eventType;

        public String subscriptionPath() {
            return broadcast ? path : "/user" + path;
        }
    }

    public static final String ACTIVE_USERS_QUERY_ENDPOINT = "/api/rpc/activeusers";

    public static final String ACTIVE_USERS_RESPONSE = "/user/rpc/activeusers";

    public static final String ACTIVITY_LOG_RESPONSE = "/user/rpc/activity";

    public static final Topic USER_CONNECTED = new Topic(false, "/topic/users/loggedin",
            UserConnectionEvent.class);

    public static final Topic USER_DISCONNECTED = new Topic(false, "/topic/users/loggedout",
            UserConnectionEvent.class);

    public static final Topic STORE_EVENTS = new Topic(false, "/topic/stores", StoreEvent.class);

    public static final Topic USER_EVENTS = new Topic(false, "/topic/users", UserEvent.class);

    public static final Topic REPO_EVENTS = new Topic(false, "/topic/repos", RepositoryEvent.class);

    public static final Topic TRANSACTION_EVENTS = new Topic(false, "/topic/transactions",
            TransactionEvent.class);

    public static final Topic PR_EVENTS = new Topic(false, "/topic/pulls", PullRequestEvent.class);

    @SuppressWarnings("unchecked")
    public static Class<? extends ServerEvent> objectTypeToType(@NonNull String eventType) {
        try {
            String className = String.format("%s.%s", ServerEvent.class.getPackage().getName(),
                    eventType);

            return (Class<? extends ServerEvent>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
