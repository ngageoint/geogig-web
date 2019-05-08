package org.geogig.web.wsclient;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.geogig.web.client.internal.auth.HttpBasicAuth;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.events.EventTopics;
import org.geogig.web.model.events.EventsJSON;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WSClient {

    private final WebSocketStompClient stompClient;

    private StompSession session;

    private String sessionIdentifier;

    private EventPublisherStompFrameHandler localEventPublisher;

    private final @Getter @NonNull URI stompURL;

    private final LocalSubscriberExceptionHandler exceptionHandler;

    private final EventBus clientEventBus;

    private final Supplier<HttpBasicAuth> authSupplier;

    /**
     * @param stompURL e.g. {@code ws://localhost:8181/ws}
     */
    public WSClient(@NonNull URI stompURL, @NonNull Supplier<HttpBasicAuth> authSupplier) {
        this.authSupplier = authSupplier;
        this.stompURL = stompURL;
        ObjectMapper objectMapper = EventsJSON.newObjectMapper();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        WebSocketClient client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);
        stompClient.setAutoStartup(false);
        stompClient.setMessageConverter(messageConverter);

        exceptionHandler = new LocalSubscriberExceptionHandler();
        clientEventBus = new EventBus(exceptionHandler);
    }

    public void connect() {
        log.debug("Connecting to websocket endpoint {}", stompURL);
        disconnect();

        final HttpBasicAuth auth = authSupplier.get();
        Preconditions.checkState(auth != null,
                "No credentials provided for websocket connection to " + stompURL);

        stompClient.start();
        session = connect(auth.getUsername(), auth.getPassword());

        sessionIdentifier = String.format("%s (%s@%s)", session.getSessionId(), auth.getUsername(),
                stompURL);

        log.info("STOMP WebSocket session connected: {}", sessionIdentifier);

        localEventPublisher = new EventPublisherStompFrameHandler(clientEventBus,
                sessionIdentifier);

        subscribeToAllEvents(session, localEventPublisher);
    }

    private StompSession connect(@NonNull String user, @NonNull String pwd) {
        String plainCredentials = String.format("%s:%s", user, pwd);
        String base64Credentials = Base64.getEncoder().encodeToString(plainCredentials.getBytes());

        final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Basic " + base64Credentials);

        StompSession session;

        final String wsURL = stompURL.toString();
        try {
            session = stompClient.connect(wsURL, headers, new StompSessionHandlerAdapter() {
                public @Override void handleException(StompSession session,
                        @Nullable StompCommand command, StompHeaders headers, byte[] payload,
                        Throwable exception) {
                    System.err.printf("Session exception %s, %s, %s", session, command, headers);
                    exception.printStackTrace();
                }
            }).get(5, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return session;
    }

    public void disconnect() {
        if (stompClient.isRunning()) {
            if (session != null && session.isConnected()) {
                session.disconnect();
                log.info("STOMP session disconnected: " + sessionIdentifier);
            }
            session = null;
            stompClient.stop();
            localEventPublisher = null;
        }
    }

    private void subscribeToAllEvents(StompSession session, StompFrameHandler handler) {
        session.subscribe(EventTopics.STORE_EVENTS.subscriptionPath(), handler);
        session.subscribe(EventTopics.USER_CONNECTED.subscriptionPath(), handler);
        session.subscribe(EventTopics.USER_DISCONNECTED.subscriptionPath(), handler);
        session.subscribe(EventTopics.USER_EVENTS.subscriptionPath(), handler);
        session.subscribe(EventTopics.TRANSACTION_EVENTS.subscriptionPath(), handler);
        session.subscribe(EventTopics.REPO_EVENTS.subscriptionPath(), handler);
        session.subscribe(EventTopics.PR_EVENTS.subscriptionPath(), handler);
    }

    /**
     * Registers an event listener that will asynchronously be notified of server push events
     * 
     * @param an object that uses Guava's eventbus {@link Subscribe @Subscribe} to listen to
     *        specific event types
     */
    public void addEventListener(@NonNull Object listener) {
        clientEventBus.register(listener);
    }

    public void removeEventListener(@NonNull Object listener) {
        clientEventBus.unregister(listener);
    }

    public <T extends ServerEvent> void addEventListener(@NonNull Class<T> eventType,
            @NonNull Consumer<T> consumer, boolean includingSubclasses) {
        addEventListener(
                new DiscriminatingEventListener<>(eventType, includingSubclasses, consumer));
    }

    public <T extends ServerEvent> void removeEventListener(@NonNull Class<T> eventType,
            @NonNull Consumer<T> consumer) {
        removeEventListener(new DiscriminatingEventListener<>(eventType, true, consumer));
    }

    public void setExceptionHandler(Consumer<Throwable> handler) {
        this.exceptionHandler.setExceptionHandler(handler);
    }

    private static class LocalSubscriberExceptionHandler implements SubscriberExceptionHandler {

        private @Setter Consumer<Throwable> exceptionHandler;

        public @Override void handleException(Throwable exception,
                SubscriberExceptionContext context) {

            log.warn("Error handling server event", exception);
            Consumer<Throwable> handler = this.exceptionHandler;
            if (handler != null) {
                handler.accept(exception);
            }
        }

    }

    private @RequiredArgsConstructor static class DiscriminatingEventListener<T extends ServerEvent> {

        private final @NonNull Class<T> eventType;

        private final boolean includeSubclasses;

        private final @NonNull Consumer<T> consumer;

        public @Subscribe void handleEvent(ServerEvent event) {
            if (includeSubclasses && eventType.isInstance(event)) {
                consumer.accept(eventType.cast(event));
            } else if (event.getClass().isAssignableFrom(eventType)) {
                consumer.accept(eventType.cast(event));
            }
        }
    }
}
