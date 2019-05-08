package org.geogig.server.websockets;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.geogig.web.model.events.EventTopics.USER_DISCONNECTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.model.User;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.UserConnectionEvent;
import org.geogig.web.model.events.EventTopics;
import org.geogig.web.model.events.EventsJSON;
import org.geogig.web.model.events.GetActiveUsersQuery;
import org.geogig.web.model.events.GetActiveUsersResponse;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

//@formatter:off
@RunWith(SpringRunner.class)
@SpringBootTest(
      classes = { ConfigTestConfiguration.class, EventsConfiguration.class, WebSocketPushEventsConfiguration.class },
      webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(
      inheritProperties = true,
      properties = {
              "spring.main.allow-bean-definition-overriding=true",
              "GEOGIG_SERVER_CONFIG_DIRECTORY=${java.io.tmpdir}/geogig-web/config/"
              }
)
//@formatter:on
public class WebSocketRPCControllerIT {

    // timeout in seconds for checking the queue, 1 or 2 should be enough, increase if debugging
    private static final long TIMEOUT_SECS = 2;

    private @LocalServerPort int port;

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired WebSocketsPushEventsService service;

    /**
     * populated by {@link #connect(String, String)}, cleared by {@link #tearDown()}
     */
    private List<StompSession> openSessions = new ArrayList<>();

    private String WEBSOCKET_URI = "ws://localhost:8181/ws";

    private WebSocketStompClient stompClient;

    public @Before void setup() {
        // stompClient = new WebSocketStompClient(new SockJsClient(
        // asList(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        ObjectMapper objectMapper = EventsJSON.newObjectMapper();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        stompClient.setMessageConverter(messageConverter);

        WEBSOCKET_URI = String.format("ws://localhost:%d/ws", port);
    }

    public @After void tearDown() {
        service.reset();
        openSessions.forEach(s -> {
            if (s.isConnected())
                s.disconnect();
        });
        openSessions.clear();
    }

    private static class ActiveUsersHandler extends StompSessionHandlerAdapter {
        final BlockingQueue<GetActiveUsersResponse> queue = new LinkedBlockingQueue<>();

        public @Override Type getPayloadType(StompHeaders headers) {
            return GetActiveUsersResponse.class;
        }

        public @Override void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((GetActiveUsersResponse) payload);
        }
    }

    public @Test final void testGetActiveUsers() throws Exception {

        User user1 = support.createUser("gabe", support.getDefaultStore());
        User user2 = support.createUser("dave", support.getDefaultStore());

        StompSession session1 = connectAs(user1);
        ActiveUsersHandler handler = new ActiveUsersHandler();
        session1.subscribe(EventTopics.ACTIVE_USERS_RESPONSE, handler);

        GetActiveUsersQuery query = new GetActiveUsersQuery();
        GetActiveUsersResponse response;
        session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);

        response = handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertEquals(1, response.getUsers().size());

        StompSession session2 = connectAs(user1);
        session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);
        response = handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertEquals(2, response.getUsers().size());

        StompSession session3 = connectAs(user2);
        session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);
        response = handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertEquals(3, response.getUsers().size());

        disconnectAndWait(session2);
        session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);
        response = handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertEquals(2, response.getUsers().size());

        disconnectAndWait(session3);
        session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);
        response = handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertEquals(1, response.getUsers().size());
    }

    public @Test final void testGetActiveUsersSentOnlyToCallerSession() throws Exception {
        User user1 = support.createUser("gabe", support.getDefaultStore());
        User user2 = support.createUser("dave", support.getDefaultStore());

        StompSession user1Session1 = connectAs(user1);
        ActiveUsersHandler user1Session1Handler = new ActiveUsersHandler();
        user1Session1.subscribe(EventTopics.ACTIVE_USERS_RESPONSE, user1Session1Handler);

        StompSession user1Session2 = connectAs(user1);
        ActiveUsersHandler user1Session2Handler = new ActiveUsersHandler();
        user1Session2.subscribe(EventTopics.ACTIVE_USERS_RESPONSE, user1Session2Handler);

        StompSession user2Session1 = connectAs(user2);
        ActiveUsersHandler user2Session1Handler = new ActiveUsersHandler();
        user2Session1.subscribe(EventTopics.ACTIVE_USERS_RESPONSE, user2Session1Handler);

        GetActiveUsersResponse response;
        GetActiveUsersQuery query = new GetActiveUsersQuery();

        user1Session1.send(EventTopics.ACTIVE_USERS_QUERY_ENDPOINT, query);
        response = user1Session1Handler.queue.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertNotNull("no response in admin subscription", response);
        assertEquals(3, response.getUsers().size());

        final String msg = "response should've only been sent to the request issuer session";
        assertTrue(msg, user1Session2Handler.queue.isEmpty());
        assertTrue(msg, user2Session1Handler.queue.isEmpty());

    }

    private void disconnectAndWait(StompSession session) throws Exception {
        StompSession admin = connectAsAdmin();
        BlockingQueue<ServerEvent> q = new LinkedBlockingQueue<>();
        admin.subscribe(USER_DISCONNECTED.path(), new EventHandler(q));
        session.disconnect();
        ServerEvent event = q.poll(TIMEOUT_SECS, TimeUnit.SECONDS);
        assertThat(event, Matchers.instanceOf(UserConnectionEvent.class));
        admin.disconnect();
        Thread.sleep(200);
    }

    private StompSession connectAs(@NonNull User user) throws Exception {
        String name = user.getIdentity();
        String pwd = "geo123";
        return connect(name, pwd);
    }

    private StompSession connectAsAdmin() throws Exception {
        return connect("admin", "g30g1g");
    }

    private StompSession connect(@NonNull String user, @NonNull String pwd) throws Exception {
        String plainCredentials = String.format("%s:%s", user, pwd);
        String base64Credentials = Base64.getEncoder().encodeToString(plainCredentials.getBytes());

        final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Basic " + base64Credentials);

        StompSession session;
        try {
            session = stompClient.connect(WEBSOCKET_URI, headers, new StompSessionHandlerAdapter() {
                public @Override void handleException(StompSession session,
                        @Nullable StompCommand command, StompHeaders headers, byte[] payload,
                        Throwable exception) {
                    System.err.printf("Session exception %s, %s, %s", session, command, headers);
                    exception.printStackTrace();
                }
            }).get(TIMEOUT_SECS, SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        openSessions.add(session);
        return session;
    }

    private static @RequiredArgsConstructor class EventHandler implements StompFrameHandler {
        private final @NonNull Queue<ServerEvent> queue;

        /**
         * @return {@link ServerEvent}.class, the {@link ObjectMapper} will take care of converting
         *         to the appropriate concrete class, but we can't return null here
         */
        public @Override Type getPayloadType(StompHeaders headers) {
            return ServerEvent.class;
        }

        public @Override void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((ServerEvent) payload);
        }

    }
}
