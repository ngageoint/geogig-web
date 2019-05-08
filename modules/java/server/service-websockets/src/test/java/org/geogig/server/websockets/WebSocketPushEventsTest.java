package org.geogig.server.websockets;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.ADDED;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.DELETED;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.MODIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.model.User;
import org.geogig.server.service.pr.PullRequestRequest;
import org.geogig.server.service.pr.PullRequestService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.geogig.web.model.PullRequestEvent;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.PullRequestStatusEvent;
import org.geogig.web.model.RepositoryEvent;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.ServerEvent.EventTypeEnum;
import org.geogig.web.model.StoreEvent;
import org.geogig.web.model.UserEvent;
import org.geogig.web.model.events.EventTopics;
import org.geogig.web.model.events.EventsJSON;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.locationtech.geogig.test.TestData;
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
public class WebSocketPushEventsTest {

    private @LocalServerPort int port;

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired RepositoryManagementService repos;

    private @Autowired PullRequestService prs;

    /**
     * populated by {@link #connect(String, String)}, cleared by {@link #tearDown()}
     */
    private List<StompSession> openSessions = new ArrayList<>();

    private String WEBSOCKET_URI = "ws://localhost:8080/ws";

    private WebSocketStompClient stompClient;

    private User user1, user2, user3;

    private RepoInfo repo1, repo1Fork, repo3;

    private TestData repo1Suport, forkSupport, repo3Support;

    private EventQueue adminEvents, user1Events, user2Events, user3Events;

    private StompSession adminSession, user1Session, user2Session, user3Session;

    public @Rule TestName testName = new TestName();

    public @Before void setup() throws Exception {
        // stompClient = new WebSocketStompClient(new SockJsClient(
        // asList(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        ObjectMapper objectMapper = EventsJSON.newObjectMapper();
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        stompClient.setMessageConverter(messageConverter);
        WEBSOCKET_URI = String.format("ws://localhost:%d/ws", port);

        user1 = support.createUser("user1");
        user2 = support.createUser("user2");
        user3 = support.createUser("user3");

        repo1 = support.runAs(user1).createRepo("natural_earth");
        repo1Suport = new TestData(repos.resolve(repo1)).loadDefaultData();

        repo1Fork = support.runAs(user2).fork(repo1);
        forkSupport = new TestData(repos.resolve(repo1Fork));

        repo3 = support.runAs(user3).createRepo("emptyrepo");
        repo3Support = new TestData(repos.resolve(repo3));

        adminEvents = new EventQueue();
        user1Events = new EventQueue();
        user2Events = new EventQueue();
        user3Events = new EventQueue();

        adminSession = connectAsAdmin();
        user1Session = connectAs(user1);
        user2Session = connectAs(user2);
        user3Session = connectAs(user3);
        subscribeToAllEvents(adminSession, adminEvents);
        subscribeToAllEvents(user1Session, user1Events);
        subscribeToAllEvents(user2Session, user2Events);
        subscribeToAllEvents(user3Session, user3Events);
        System.err.printf(">> ---------- %s ----------\n", testName.getMethodName());
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

    public @After void tearDown() {
        System.err.printf("<< ---------- %s ----------\n", testName.getMethodName());
        openSessions.forEach(s -> {
            if (s.isConnected())
                s.disconnect();
        });
        openSessions.clear();
    }

    public @Test final void testStoreCreate() throws InterruptedException {
        Store store = support.runAs("admin").createStore("store1");
        ServerEvent event = adminEvents.poll(1, TimeUnit.SECONDS);
        assertThat(event, Matchers.instanceOf(StoreEvent.class));
        assertEquals(store.getIdentity(), ((StoreEvent) event).getSubject().getIdentity());
        assertEquals(EventTypeEnum.ADDED, event.getEventType());
        assertNotNull(event.getCaller());
        assertEquals("admin", event.getCaller().getIdentity());

        assertTrue(user1Events.isEmpty());
        assertTrue(user2Events.isEmpty());
        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testStoreUpdate() throws InterruptedException {
        Store store = support.getDefaultStore();
        store.setDescription("new description");
        Store modified = support.runAs("admin").update(store);

        ServerEvent event = adminEvents.poll(1, TimeUnit.SECONDS);
        assertThat(event, Matchers.instanceOf(StoreEvent.class));
        assertEquals(store.getIdentity(), ((StoreEvent) event).getSubject().getIdentity());
        assertEquals(modified.getDescription(), ((StoreEvent) event).getSubject().getDescription());
        assertEquals(EventTypeEnum.MODIFIED, event.getEventType());
        assertNotNull(event.getCaller());
        assertEquals("admin", event.getCaller().getIdentity());
        assertTrue(user1Events.isEmpty());
        assertTrue(user2Events.isEmpty());
        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testStoreDelete() throws InterruptedException {
        Store store = support.runAs("admin").createStore("store1");
        ServerEvent event = adminEvents.poll(1, TimeUnit.SECONDS);

        adminEvents.clear();
        Store deleted = support.delete(store);
        event = adminEvents.poll(1, TimeUnit.SECONDS);

        assertThat(event, Matchers.instanceOf(StoreEvent.class));
        assertEquals(deleted.getIdentity(), ((StoreEvent) event).getSubject().getIdentity());
        assertEquals(DELETED, event.getEventType());
        assertNotNull(event.getCaller());
        assertEquals("admin", event.getCaller().getIdentity());

        assertTrue(user1Events.isEmpty());
        assertTrue(user2Events.isEmpty());
        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testUserCreate() throws Exception {
        User newUser = support.runAs("admin").createUser("someUser");

        assertUserEvent(adminEvents, ADDED, null, newUser, "admin should be notified");

        assertTrue(user1Events.isEmpty());
        assertTrue(user2Events.isEmpty());
        assertTrue(user3Events.isEmpty());
    }

    @Ignore // not sure why this works if debugging but doesn't otherwise
    public @Test final void testUserUpdate() throws Exception {
        user1.setFullName("Gabriel Roldan");
        User modified = support.runAs(user2).update(user1);

        assertUserEvent(adminEvents, MODIFIED, user2, modified, "admin should be notified");
        assertUserEvent(user1Events, MODIFIED, user2, modified, "modified user should be notified");
        assertUserEvent(user2Events, MODIFIED, user2, modified, "caller should be notified");

        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testUserDelete() throws Exception {
        support.runAs("admin").getRepos(user1).forEach(r -> support.delete(r));
        Thread.sleep(1000);
        adminEvents.clear();
        user2Events.clear();
        user1Events.clear();

        User user = support.runAs(user2).delete(user1);
        assertNotNull(user);

        assertUserEvent(adminEvents, DELETED, user2, user1, "admin should be notified");
        assertUserEvent(user2Events, DELETED, user2, user1, "caller should be notified");

        assertTrue(user3Events.isEmpty());
        assertTrue(user1Events.isEmpty());
    }

    private void assertUserEvent(EventQueue queue, EventTypeEnum eventType, User expectedCaller,
            User expectedUser, String message) throws Exception {

        ServerEvent event = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(message, event);
        assertThat(event, Matchers.instanceOf(UserEvent.class));
        assertUserEvent((UserEvent) event, eventType, expectedCaller, expectedUser);
    }

    private void assertUserEvent(UserEvent event, EventTypeEnum eventType, User expectedCaller,
            User expectedUser) {

        assertEquals(expectedUser.getIdentity(), ((UserEvent) event).getSubject().getIdentity());
        assertEquals(eventType, event.getEventType());
        assertNotNull(event.getCaller());
        if (expectedCaller == null) {
            assertEquals("admin", event.getCaller().getIdentity());
        } else {
            assertEquals(expectedCaller.getIdentity(), event.getCaller().getIdentity());
        }
    }

    public @Test final void testRepositoryCreate() throws Exception {
        RepoInfo repo = support.runAs(user1).createRepo("newrepo_user1");
        assertRepositoryEvent(user1Events, ADDED, user1, repo, "caller should be notified");
        assertRepositoryEvent(adminEvents, ADDED, user1, repo, "admin should be notified");
        assertTrue(user2Events.isEmpty());
        assertTrue(user3Events.isEmpty());
    }

    @Ignore // not sure why this works if debugging but doesn't otherwise
    public @Test final void testRepositoryUpdate() throws Exception {
        repo1.setDescription("renamed");
        // make user2 do the update, whereas user1 is the repo owner
        // RepoInfo updated = CompletableFuture.supplyAsync(() ->
        // support.runAs(user2).update(repo1)).get();
        RepoInfo updated = support.runAs(user2).update(repo1);
        //
        // verify(pushService, timeout(5000).atLeastOnce())
        // .pushRepositoryEvent(any(RepositoryEvent.class));

        assertRepositoryEvent(adminEvents, MODIFIED, user2, updated,
                "admin should have been notified");

        assertRepositoryEvent(user1Events, MODIFIED, user2, updated,
                "owner should have been notified");

        assertRepositoryEvent(user2Events, MODIFIED, user2, updated,
                "caller should have been notified");

        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testRepositoryDelete() throws Exception {
        RepoInfo deleted = support.runAs(user3).delete(repo3);

        assertRepositoryEvent(adminEvents, DELETED, user3, deleted,
                "admin should have been notified");

        assertRepositoryEvent(user3Events, DELETED, user3, deleted,
                "owner should have been notified");

        assertTrue(user1Events.isEmpty());
        assertTrue(user2Events.isEmpty());
    }

    private void assertRepositoryEvent(EventQueue queue, EventTypeEnum eventType,
            User expectedCaller, RepoInfo expectedRepo, String message) throws Exception {

        ServerEvent event = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(message, event);
        assertThat(event, Matchers.instanceOf(RepositoryEvent.class));
        assertRepositoryEvent((RepositoryEvent) event, eventType, expectedCaller, expectedRepo);
    }

    private void assertRepositoryEvent(RepositoryEvent event, EventTypeEnum eventType,
            User expectedCaller, RepoInfo expectedRepo) {

        assertEquals(expectedRepo.getIdentity(),
                ((RepositoryEvent) event).getSubject().getIdentity());
        assertEquals(eventType, event.getEventType());
        assertNotNull(event.getCaller());
        assertEquals(expectedCaller.getIdentity(), event.getCaller().getIdentity());
    }

    public @Test final void testPullRequestCreate() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");

        final User issuer = user2;
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(repo1Fork.getId()).issuerUser(issuer.getId()).targetBranch("master")
                .targetRepo(repo1.getId()).title("a cool pull request").build();

        support.runAs(issuer);
        PullRequest pr = prs.create(request);

        assertPREvent(adminEvents, ADDED, issuer, pr, "admin should be notified");
        assertPREvent(user2Events, ADDED, issuer, pr, "caller should be notified");
        assertPREvent(user1Events, ADDED, issuer, pr, "target repo owner should be notified");

        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testPullRequestUpdate() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");

        final User issuer = user2;
        final RepoInfo target = repo1;
        final RepoInfo source = repo1Fork;
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(source.getId()).issuerUser(issuer.getId()).targetBranch("master")
                .targetRepo(target.getId()).title("a cool pull request").build();

        support.runAs(issuer);
        PullRequest pr = prs.create(request);
        assertPREvent(adminEvents, ADDED, issuer, pr, "admin should be notified");
        assertPREvent(user2Events, ADDED, issuer, pr, "caller should be notified");
        assertPREvent(user1Events, ADDED, issuer, pr, "target repo owner should be notified");

        PullRequest updated = prs.updatePullRequest(user1.getIdentity(), target.getIdentity(),
                pr.getId(), "new title", "new description", true, null);

        assertPREvent(adminEvents, MODIFIED, issuer, updated, "admin should be notified");
        assertPREvent(user2Events, MODIFIED, issuer, updated, "caller should be notified");
        assertPREvent(user1Events, MODIFIED, issuer, updated,
                "target repo owner should be notified");

        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testPullRequestClose() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");

        final User issuer = user2;
        final RepoInfo target = repo1;
        final RepoInfo source = repo1Fork;
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(source.getId()).issuerUser(issuer.getId()).targetBranch("master")
                .targetRepo(target.getId()).title("a cool pull request").build();

        support.runAs(issuer);
        PullRequest pr = prs.create(request);
        assertPREvent(adminEvents, ADDED, issuer, pr, "admin should be notified");
        assertPREvent(user2Events, ADDED, issuer, pr, "caller should be notified");
        assertPREvent(user1Events, ADDED, issuer, pr, "target repo owner should be notified");

        PullRequest closed = prs.close(user1.getId(), pr);

        assertPREvent(adminEvents, MODIFIED, user1, closed, "admin should be notified");
        assertPREvent(user2Events, MODIFIED, user1, closed, "caller should be notified");
        assertPREvent(user1Events, MODIFIED, user1, closed, "target repo owner should be notified");

        assertTrue(user3Events.isEmpty());
    }

    public @Test final void testPullRequestMerge() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");

        final User issuer = user2;
        final RepoInfo target = repo1;
        final RepoInfo source = repo1Fork;
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(source.getId()).issuerUser(issuer.getId()).targetBranch("master")
                .targetRepo(target.getId()).title("a cool pull request").build();

        support.runAs(issuer);
        PullRequest pr = prs.create(request);
        assertPREvent(adminEvents, ADDED, issuer, pr, "admin should be notified");
        assertPREvent(user2Events, ADDED, issuer, pr, "caller should be notified");
        assertPREvent(user1Events, ADDED, issuer, pr, "target repo owner should be notified");

        prs.merge(user1, pr);

        assertPREvent(adminEvents, MODIFIED, user1, pr, "admin should be notified");
        assertPREvent(user2Events, MODIFIED, user1, pr, "caller should be notified");
        assertPREvent(user1Events, MODIFIED, user1, pr, "target repo owner should be notified");

        assertTrue(user3Events.isEmpty());
    }

    private void assertPREvent(EventQueue queue, EventTypeEnum eventType, User expectedCaller,
            PullRequest expected, String message) throws Exception {

        ServerEvent event = queue.poll(2, TimeUnit.SECONDS);
        while (event != null && !PullRequestEvent.class.equals(event.getClass())) {
            event = queue.poll(2, TimeUnit.SECONDS);
        }
        assertNotNull(message, event);
        assertThat(event, Matchers.instanceOf(PullRequestEvent.class));

        PullRequestEvent prEvent = (PullRequestEvent) event;
        PullRequestInfo pr = prEvent.getSubject();
        assertEquals(expected.getId(), pr.getId());
        assertEquals(eventType, event.getEventType());
        assertNotNull(event.getCaller());
        assertEquals(expectedCaller.getIdentity(), event.getCaller().getIdentity());
    }

    public @Test final void testPullRequestStatusOnCreate() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(repo1Fork.getId()).issuerUser(user2.getId()).targetBranch("master")
                .targetRepo(repo1.getId()).title("a cool pull request").build();

        PullRequest pr = prs.create(request);
        assertPRStatusEvent(adminEvents, pr, "admin should be notified");
        assertPRStatusEvent(user2Events, pr, "issuer repo owner should be notified");
        assertPRStatusEvent(user1Events, pr, "target repo owner should be notified");
    }

    public @Test final void testPullRequestStatusOnTargetBranchChange() throws Exception {
        forkSupport.insert(TestData.point1_modified).add().commit("pr commit");
        PullRequestRequest request = PullRequestRequest.builder().issuerBranch("master")
                .issuerRepo(repo1Fork.getId()).issuerUser(user2.getId()).targetBranch("master")
                .targetRepo(repo1.getId()).title("a cool pull request").build();

        PullRequest pr = prs.create(request);
        assertPRStatusEvent(adminEvents, pr, "admin should be notified");
        assertPRStatusEvent(user2Events, pr, "issuer repo owner should be notified");
        assertPRStatusEvent(user1Events, pr, "target repo owner should be notified");

        repo1Suport.insert(TestData.point2_modified).add().commit("commit on target branch");
        adminEvents.clear();
        user2Events.clear();
        user1Events.clear();
        assertPRStatusEvent(adminEvents, pr, "admin should be notified");
        assertPRStatusEvent(user2Events, pr, "issuer repo owner should be notified");
        assertPRStatusEvent(user1Events, pr, "target repo owner should be notified");
    }

    private void assertPRStatusEvent(EventQueue queue, PullRequest expected, String message)
            throws Exception {

        ServerEvent event = queue.poll(2, TimeUnit.SECONDS);
        while (event != null && !PullRequestStatusEvent.class.equals(event.getClass())) {
            event = queue.poll(2, TimeUnit.SECONDS);
        }
        assertNotNull(message, event);
        assertThat(event, Matchers.instanceOf(PullRequestStatusEvent.class));

        PullRequestStatusEvent stRvent = (PullRequestStatusEvent) event;
        PullRequestInfo pr = stRvent.getSubject();
        PullRequestStatus status = stRvent.getStatus();
        assertNotNull(status);
        assertEquals(expected.getId(), pr.getId());
        assertEquals(MODIFIED, event.getEventType());
        assertNull(event.getCaller());
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
            }).get(2, SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        openSessions.add(session);
        return session;
    }

    private static class EventQueue implements StompFrameHandler {

        private final BlockingQueue<ServerEvent> queue = new LinkedBlockingQueue<>();

        /**
         * @return {@link ServerEvent}.class, the {@link ObjectMapper} will take care of converting
         *         to the appropriate concrete class, but we can't return null here
         */
        public @Override Type getPayloadType(StompHeaders headers) {
            return ServerEvent.class;
        }

        public ServerEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
            return queue.poll(timeout, unit);
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public void clear() {
            queue.clear();
        }

        public @Override void handleFrame(StompHeaders headers, Object payload) {
            ServerEvent event = (ServerEvent) payload;
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.err.printf("Got message %s %s %s %s\n", event.getEventType(),
                    event.getClass().getSimpleName(), LocalTime.now(),
                    Thread.currentThread().getName());
        }

    }
}
