package org.geogig.web.integration.client;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.TWO_SECONDS;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.ADDED;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.DELETED;
import static org.geogig.web.model.ServerEvent.EventTypeEnum.MODIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Client;
import org.geogig.web.client.PullRequest;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.ForkEvent;
import org.geogig.web.model.PullRequestEvent;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.RepositoryEvent;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.ServerEvent;
import org.geogig.web.model.ServerEvent.EventTypeEnum;
import org.geogig.web.model.StoreEvent;
import org.geogig.web.model.UserEvent;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.springframework.beans.BeanWrapperImpl;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketEventsIntegrationTest extends AbstractIntegrationTest {

    private Store store;

    private org.geogig.web.client.User admin, user1, user2;

    private Repo user1repo1;

    org.geogig.web.wsclient.Client adminWsClient, user1WsClient, user2WsClient;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Rule TestName testName = new TestName();

    protected @Override Function<String, Client> getClientFactory() {
        return url -> new org.geogig.web.wsclient.Client(url);
    }

    public @Before void before() {
        log.info(">>>>>>>>>>>>>>>> {} <<<<<<<<<<<<<<<<<<<", testName.getMethodName());
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        admin = testSupport.getAdmin();
        user1 = testSupport.createUser("user1", storeName);
        user2 = testSupport.createUser("user2", storeName);

        user1repo1 = user1.createRepo("user1repo1");
        adminWsClient = (org.geogig.web.wsclient.Client) testSupport.getAdminClient();
        user1WsClient = (org.geogig.web.wsclient.Client) user1.getClient();
        user2WsClient = (org.geogig.web.wsclient.Client) user2.getClient();
    }

    public @After void after() {
        log.info("<<<<<<<<<<<<<<<<<<< {} >>>>>>>>>>>>>>>>", testName.getMethodName());
    }

    private org.geogig.web.wsclient.Client client(User user) {
        return (org.geogig.web.wsclient.Client) user.getClient();
    }

    public @Test void testCreateModifyDeleteStore() throws InterruptedException {
        AtomicReference<StoreEvent> reference = addListener(admin, StoreEvent.class, false);

        Store store = testSupport.createStore("wsstoretest", "WebSocket event store test");
        assertEvent(reference, ADDED, admin, store.getInfo());

        clear(reference);
        store.getInfo().setDescription("modified description");

        store.save();
        assertEvent(reference, MODIFIED, admin, store.getInfo());

        clear(reference);
        store.getInfo().setDescription("modified description");

        assertTrue(store.remove());
        assertEvent(reference, DELETED, admin, store.getInfo());
    }

    public @Test void testCreateModifyDeleteUser() {
        AtomicReference<UserEvent> reference = addListener(admin, UserEvent.class, false);

        User user = testSupport.createUser("dave", "s3cr3t", "Dave", "dave@example.com",
                store.getIdentity());
        assertEvent(reference, ADDED, admin, user.getInfo());

        user.setFullName("David Blasby");
        clear(reference);
        user.modify();

        assertEvent(reference, MODIFIED, user, user.getInfo());

        clear(reference);
        adminWsClient.users().delete(user.getIdentity());
        assertEvent(reference, MODIFIED, admin,
                e -> user.getInfo().getId().equals(e.getSubject().getId()));
    }

    public @Test void testCreateModifyDeleteRepo() {
        Class<RepositoryEvent> type = RepositoryEvent.class;
        AtomicReference<RepositoryEvent> adminRef = addListener(admin, type, false);
        AtomicReference<RepositoryEvent> user1Ref = addListener(user1, type, false);
        AtomicReference<RepositoryEvent> user2Ref = addListener(user2, type, false);

        Repo repo = user1.createRepo("newrepo");
        assertEvent(user1Ref, ADDED, user1, repo.getInfo());
        assertEvent(adminRef, ADDED, user1, repo.getInfo());
        assertNull(user2Ref.get());

        clear(adminRef, user1Ref);
        repo.getInfo().setDescription("modified repo description");
        repo.modify();
        assertEvent(user1Ref, MODIFIED, user1, repo.getInfo());
        assertEvent(adminRef, MODIFIED, user1, repo.getInfo());
        assertNull(user2Ref.get());

        clear(adminRef, user1Ref);
        adminWsClient.repositories().delete(user1.getIdentity(), repo.getIdentity());
        Consumer<RepositoryEvent> validator = e -> {
            assertEquals(repo.getInfo().getId(), e.getSubject().getId());
        };
        assertEvent(user1Ref, DELETED, admin, validator);
        assertEvent(adminRef, DELETED, admin, validator);
        assertNull(user2Ref.get());
    }

    public @Test void testCreateModifyDeleteFork() {
        final Repo origin = user1repo1;
        RevisionFeatureType layer = testSupport.poiFeatureType();
        testSupport.worker(origin).startTransaction().createLayer(layer)
                .commitTransaction("empty layer").startTransaction()
                .insert(layer.getName(), testSupport.poiFeatures()).commitTransaction("initial");

        Class<RepositoryEvent> type = RepositoryEvent.class;
        // true to catch repo and fork event
        AtomicReference<RepositoryEvent> adminRef = addListener(admin, type, true);
        AtomicReference<RepositoryEvent> user1Ref = addListener(user1, type, true);
        AtomicReference<RepositoryEvent> user2Ref = addListener(user2, type, true);

        user2.repositories().fork(origin).awaitTermination();
        final Repo fork = user2.getRepo(origin.getIdentity());

        Consumer<RepositoryEvent> validator = e -> {
            assertThat(e, Matchers.instanceOf(ForkEvent.class));
            ForkEvent fe = (ForkEvent) e;
            RepositoryInfo subject = fe.getSubject();
            assertNotNull(subject);
            assertNotNull(subject.getForkedFrom());
            assertEquals(origin.getId(), subject.getForkedFrom().getId());
            assertEquals(fork.getInfo(), subject);
        };

        assertEvent(user2Ref, ADDED, user2, validator);// fork owner should be notified
        assertEvent(user1Ref, ADDED, user2, validator);// owner of parent repo should be notified
        assertEvent(adminRef, ADDED, user2, validator);// admin should be notified

        clear(adminRef, user1Ref, user2Ref);
        fork.getInfo().setDescription("modified fork description");
        fork.modify();
        assertEvent(user2Ref, MODIFIED, user2, fork.getInfo());// owner should be notified
        assertEvent(adminRef, MODIFIED, user2, fork.getInfo());// admin should be notified
        assertNull(user1Ref.get());// owner of parent doesn't need to be notified that the fork
                                   // changed

        clear(adminRef, user1Ref, user2Ref);
        user2.repositories().delete(user2.getIdentity(), fork.getIdentity());
        Consumer<RepositoryEvent> deleteValidator = e -> {
            assertEquals(fork.getInfo().getId(), e.getSubject().getId());
        };
        assertEvent(user2Ref, DELETED, user2, deleteValidator);
        assertEvent(adminRef, DELETED, user2, deleteValidator);
        assertNull(user1Ref.get());// owner of parent doesn't need to be notified that the fork
                                   // changed
    }

    /**
     * Modifying or deleting a repo with forks and forks of forks, should result in events sent only
     * to direct forks, not forks of forks
     */
    public @Test void testModifyDeleteRepositoryNotifiesDirectForks() {
        //@formatter:off
        /*
         * root <--- fork1 <--- fork1_1
         *     ^
         *     |_ fork2
         */
        //@formatter:on
        final Repo root = user1repo1;
        final User rootOwner = user1, fork1Owner = user2, fork2Owner, fork1_1Owner;
        final Repo fork1, fork2, fork1_1;
        {
            fork2Owner = testSupport.createUser("fork2Owner", store.getIdentity());
            fork1_1Owner = testSupport.createUser("fork1_1Owner", store.getIdentity());
            RevisionFeatureType layer = testSupport.poiFeatureType();
            testSupport.worker(root).startTransaction().createLayer(layer)
                    .commitTransaction("empty layer").startTransaction()
                    .insert(layer.getName(), testSupport.poiFeatures())
                    .commitTransaction("initial");
        }
        fork1Owner.getClient().repositories().fork(root).awaitTermination();
        fork1 = fork1Owner.getRepo(root.getIdentity());
        fork1_1Owner.getClient().repositories().fork(fork1).awaitTermination();
        fork1_1 = fork1Owner.getRepo(root.getIdentity());

        fork2Owner.getClient().repositories().fork(root).awaitTermination();
        fork2 = fork2Owner.getRepo(root.getIdentity());

        Class<RepositoryEvent> type = RepositoryEvent.class;
        AtomicReference<RepositoryEvent> adminRef = addListener(admin, type, true);
        AtomicReference<RepositoryEvent> rootRef = addListener(rootOwner, type, true);
        AtomicReference<RepositoryEvent> fork1Ref = addListener(fork1Owner, type, true);
        AtomicReference<RepositoryEvent> fork2Ref = addListener(fork2Owner, type, true);
        AtomicReference<RepositoryEvent> fork1_1Ref = addListener(fork1_1Owner, type, true);

        root.getInfo().setDescription("modified root repo description");
        root.modify();
        assertEvent(rootRef, MODIFIED, rootOwner, root.getInfo());
        assertEvent(adminRef, MODIFIED, rootOwner, root.getInfo());
        assertEvent(fork1Ref, MODIFIED, rootOwner, root.getInfo());
        assertEvent(fork2Ref, MODIFIED, rootOwner, root.getInfo());
        assertNull(fork1_1Ref.get());

        clear(adminRef, rootRef, fork1Ref, fork2Ref, fork1_1Ref);
        rootOwner.repositories().delete(rootOwner.getIdentity(), root.getIdentity());
        Consumer<RepositoryEvent> deleteValidator = e -> {
            assertEquals(root.getInfo().getId(), e.getSubject().getId());
        };
        assertEvent(rootRef, DELETED, rootOwner, deleteValidator);
        assertEvent(adminRef, DELETED, rootOwner, deleteValidator);
        assertEvent(fork1Ref, DELETED, rootOwner, deleteValidator);
        assertEvent(fork2Ref, DELETED, rootOwner, deleteValidator);
        assertNull(fork1_1Ref.get());

        // now modify fork1, only fork1_1 should be notified, fork2 is unrelated
        clear(adminRef, rootRef, fork1Ref, fork2Ref, fork1_1Ref);
        fork1.getInfo().setDescription("modified fork1 repo description");
        fork1.modify();
        assertEvent(adminRef, MODIFIED, fork1Owner, fork1.getInfo());
        assertEvent(fork1Ref, MODIFIED, fork1Owner, fork1.getInfo());
        assertEvent(fork1_1Ref, MODIFIED, fork1Owner, fork1.getInfo());
        assertNull(rootRef.get());
        assertNull(fork2Ref.get());

        clear(adminRef, rootRef, fork1Ref, fork2Ref, fork1_1Ref);
        fork1Owner.repositories().delete(fork1Owner.getIdentity(), fork1.getIdentity());

        deleteValidator = e -> {
            assertEquals(fork1.getInfo().getId(), e.getSubject().getId());
        };
        assertEvent(adminRef, DELETED, fork1Owner, deleteValidator);
        assertEvent(fork1Ref, DELETED, fork1Owner, deleteValidator);
        assertEvent(fork1_1Ref, DELETED, fork1Owner, deleteValidator);
        assertNull(rootRef.get());
        assertNull(fork2Ref.get());

    }

    public @Test void testCreateModifyCloseMergePullRequest() {
        final User originOwner = user1, forkOwner = user2, unrelatedUser;
        final Branch issuerBranch, targetBranch;
        final Repo origin = user1repo1, fork;
        {
            unrelatedUser = testSupport.createUser("unrelated", store.getIdentity());
            RevisionFeatureType layer = testSupport.poiFeatureType();
            testSupport.worker(origin).startTransaction().createLayer(layer)
                    .commitTransaction("empty layer").startTransaction()
                    .insert(layer.getName(), testSupport.poiFeatures())
                    .commitTransaction("initial");

            forkOwner.repositories().fork(origin).awaitTermination();
            fork = forkOwner.getRepo(origin.getIdentity());

            testSupport.worker(fork).startTransaction().branch("master", "branch1")
                    .insert(layer.getName(), testSupport.poiFeature(10))
                    .commitTransaction("commit on fork on branch branch1");

            issuerBranch = fork.branches().get("branch1");
            targetBranch = origin.branches().get("master");
        }
        Class<PullRequestEvent> eventType = PullRequestEvent.class;
        // false to ignore pr status events which are subclasses of prevent
        AtomicReference<PullRequestEvent> adminRef = addListener(admin, eventType, false);
        AtomicReference<PullRequestEvent> originRef = addListener(originOwner, eventType, false);
        AtomicReference<PullRequestEvent> forkRef = addListener(forkOwner, eventType, false);
        AtomicReference<PullRequestEvent> unrelatedRef = addListener(unrelatedUser, eventType,
                false);

        final PullRequest pullRequest = issuerBranch.pullRequestTo(targetBranch, "pr title",
                "pr description");

        assertEvent(forkRef, ADDED, forkOwner, pullRequest.getInfo());
        assertEvent(originRef, ADDED, forkOwner, pullRequest.getInfo());
        assertEvent(adminRef, ADDED, forkOwner, pullRequest.getInfo());
        assertNull(unrelatedRef.get());

        clear(adminRef, originRef, forkRef);

        pullRequest.setTitle("new title");

        assertEvent(forkRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(originRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(adminRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertNull(unrelatedRef.get());

        clear(adminRef, originRef, forkRef);
        pullRequest.close();

        assertEvent(forkRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(originRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(adminRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertNull(unrelatedRef.get());

        clear(adminRef, originRef, forkRef);
        pullRequest.reOpen();

        assertEvent(forkRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(originRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(adminRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertNull(unrelatedRef.get());

        clear(adminRef, originRef, forkRef);
        PullRequestStatus result = pullRequest.merge().awaitTermination().getResult();

        assertEvent(forkRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(originRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertEvent(adminRef, MODIFIED, forkOwner, pullRequest.getInfo());
        assertNull(unrelatedRef.get());
}

    private <T extends ServerEvent> AtomicReference<T> addListener(User user, Class<T> eventType,
            boolean includingSubclasses) {

        AtomicReference<T> atomicRef = new AtomicReference<>();
        client(user).addServerEventListener(eventType, e -> atomicRef.set(e), includingSubclasses);
        return atomicRef;
    }

    private void clear(AtomicReference<?>... refs) {
        Lists.newArrayList(refs).forEach(ref -> ref.set(null));
    }

    private <T extends ServerEvent> void assertEvent(AtomicReference<T> atomicRef,
            EventTypeEnum eventType, @Nullable User expectedCaller, Object expectedSubject) {

        Consumer<T> validator = null;

        if (null != expectedSubject) {
            validator = event -> {
                Object subject = new BeanWrapperImpl(event).getPropertyValue("subject");
                assertEquals(expectedSubject, subject);
            };
        }
        assertEvent(atomicRef, eventType, expectedCaller, validator);
    }

    private <T extends ServerEvent> void assertEvent(AtomicReference<T> atomicRef,
            EventTypeEnum eventType, @Nullable User expectedCaller,
            @Nullable Consumer<T> validator) {

        await().atMost(TWO_SECONDS).untilAtomic(atomicRef, Matchers.notNullValue());
        T event = atomicRef.get();

        assertNotNull("event has no timestamp", event.getTimestamp());
        if (null == expectedCaller) {
            assertNull("expected null caller, got " + event.getCaller(), event.getCaller());
        } else {
            assertNotNull("expected caller, got null: " + event, event.getCaller());
            assertEquals(
                    String.format("expected caller %s, got %s", expectedCaller.getIdentity(),
                            event.getCaller().getIdentity()),
                    expectedCaller.getId(), event.getCaller().getId());
        }
        if (validator != null) {
            validator.accept(event);
        }
    }
}
