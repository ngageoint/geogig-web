package org.geogig.server.events.aop;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_SECOND;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.awaitility.Duration;
import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.events.model.Event;
import org.geogig.server.events.model.PullRequestEvent;
import org.geogig.server.events.model.PullRequestEvent.Created;
import org.geogig.server.events.model.PullRequestEvent.Updated;
import org.geogig.server.events.model.PullRequestStatusEvent;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.pr.PullRequestRequest;
import org.geogig.server.service.pr.PullRequestService;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geogig.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ConfigTestConfiguration.class, EventsConfiguration.class })
@DataJpaTest
@Transactional(propagation = Propagation.NEVER) // or other threads don't see db updates
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class PullRequestServiceEventsAspectTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired CatchAllEventsSubscriber catchAll;

    private @SpyBean PullRequestServiceEventsAspect aspect;

    private @Autowired PullRequestService service;

    private User user1, user2;

    /**
     * <pre>
     * <code>
     *             (adds Points/2, Lines/2, Polygons/2)
     *    branch1 o-------------------------------------
     *           /                                      \
     *          /                                        \  no ff merge
     *  master o------------------------------------------o-----------------o
     *          \  (initial commit has                                     / no ff merge
     *           \     Points/1, Lines/1, Polygons/1)                     /
     *            \                                                      /
     *             \                                                    /
     *     branch2  o--------------------------------------------------
     *             (adds Points/3, Lines/3, Polygons/3)
     *
     * </code>
     * </pre>
     * 
     * @see TestData#loadDefaultData()
     */
    private RepoInfo repo1, repo2;

    private TestData repo1Support, repo2Support;

    private PullRequestRequest request;

    public @Before void before() throws Exception {
        user1 = support.createUser("gabe");
        user2 = support.createUser("dave");
        repo1 = support.createRepo(user1, "repo");

        repo1Support = new TestData(support.getRepository(repo1));
        repo1Support.loadDefaultData();

        repo2 = support.runAs(user2).fork(repo1);
        repo2Support = new TestData(support.getRepository(repo2));

        repo2Support.insert(TestData.point2_modified).add().commit("issued pr commit");

        request = PullRequestRequest.builder()//
                .issuerRepo(repo2.getId())//
                .issuerUser(user2.getId())//
                .issuerBranch("master")//
                .targetRepo(repo1.getId())//
                .targetBranch("master")//
                .title("my PR")//
                .build();

        catchAll.clear();
    }

    public @Test final void testCreate() {
        testCreateWithCaller(user1);
        testCreateWithCaller(user2);
    }

    public @Test final void testCreateFailedProducesNoEvent() {
        UUID invalidRepoId = UUID.randomUUID();
        request = request.withTargetRepo(invalidRepoId);
        catchAll.clear();
        try {
            service.create(request);
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
            assertTrue(true);
        }
        assertEquals(0, catchAll.size());
    }

    private void testCreateWithCaller(User caller) {
        support.runAs(caller);
        catchAll.clear();
        request = request.withIssuerUser(caller.getId());
        PullRequest pr = service.create(request);
        assertEquals(caller.getIdentity(), pr.getCreatedBy());// verify support.runAs(caller) worked

        assertThat(catchAll.size(), greaterThan(0));
        Created event = catchAll.last(PullRequestEvent.Created.class);
        assertNotNull(event);
        assertEquals(pr, event.getRequest());
        assertTrue(event.getCaller().isPresent());
        assertEquals(request.getIssuerUser(), event.getCaller().get().getId());
    }

    public @Test final void testClose() {
        PullRequest pr = service.create(request);
        assertThat(catchAll.size(), greaterThan(0));

        PullRequest closed = service.close(user1.getId(), pr);
        assertThat(catchAll.size(), greaterThan(1));

        Updated event = catchAll.last(PullRequestEvent.Updated.class);
        assertNotNull(event);
        assertTrue(event.getCaller().isPresent());
        assertEquals(user1.getId(), event.getCaller().get().getId());

        PullRequest eventReq = event.getRequest();
        assertNotNull(eventReq);
        assertEquals(closed, eventReq);
    }

    public @Test final void testMerge() {
        PullRequest pr = service.create(request);
        assertThat(catchAll.size(), greaterThan(0));

        Task<PullRequestStatus> task = service.merge(user1, pr);
        task.getFuture().join();

        assertThat(catchAll.size(), greaterThan(1));
        await().atMost(ONE_SECOND)
                .until(() -> catchAll.findLast(PullRequestEvent.Updated.class).isPresent());

        Updated event = catchAll.findLast(PullRequestEvent.Updated.class).get();
        assertNotNull(event);

        PullRequest eventReq = event.getRequest();
        assertNotNull(eventReq);
        assertEquals(pr.getId(), eventReq.getId());
        assertEquals(PullRequest.Status.MERGED, eventReq.getStatus());

        assertTrue(event.getCaller().isPresent());
        assertEquals(user1.getId(), event.getCaller().get().getId());
    }

    public @Test final void testPrStatusChangedEvent() {
        List<PullRequestStatusEvent> events;

        PullRequest pr = service.create(request);
        events = waitForCount(PullRequestStatusEvent.class, 1);

        SimpleFeature sourceChange = TestData.clone(TestData.point1_modified);
        sourceChange.setAttribute("sp", "changed on issuer branch");
        repo2Support.insert(sourceChange).add().commit("changed issuer branch");
        events = waitForCount(PullRequestStatusEvent.class, 2);
        // repo2Support.log("master").forEachRemaining(System.err::println);

        SimpleFeature conflicted = TestData.clone(TestData.point1_modified);
        conflicted.setAttribute("sp", "changed on target branch");
        repo1Support.insert(conflicted).add().commit("changed target branch");
        // repo1Support.log("master").forEachRemaining(System.err::println);

        events = waitForCount(PullRequestStatusEvent.class, 3);
        PullRequestStatusEvent conflictStatus = events.get(2);
        assertEquals(1, conflictStatus.getStatus().getNumConflicts());

        final Task<PullRequestStatus> mergeFailsTask = service.merge(user1, pr);
        await().atMost(FIVE_SECONDS).until(() -> mergeFailsTask.getFuture().isDone());

        events = waitForCount(PullRequestStatusEvent.class, 4);
        PullRequestStatusEvent mergeFailedStatus = events.get(3);
        assertFalse(mergeFailedStatus.getStatus().isMerged());
        assertEquals(1, mergeFailedStatus.getStatus().getNumConflicts());

        // fix conflict
        repo1Support.insert(sourceChange).add().commit("fix conflict by updating target branch");
        events = waitForCount(PullRequestStatusEvent.class, 5);
        PullRequestStatusEvent conflictFixedStatus = events.get(4);
        assertFalse(conflictFixedStatus.getStatus().isMerged());
        assertEquals(0, conflictFixedStatus.getStatus().getNumConflicts());

        // re-merge
        Task<PullRequestStatus> mergeTask = service.merge(user1, pr);
        await().atMost(Duration.FOREVER).until(() -> mergeTask.getFuture().isDone());

        events = waitForCount(PullRequestStatusEvent.class, 7);
        PullRequestStatusEvent mergedStatus = events.get(6);
        assertEquals(0, mergedStatus.getStatus().getNumConflicts());
        assertTrue(mergedStatus.getStatus().isMerged());
    }

    private <T extends Event> List<T> waitForCount(Class<T> type, int expectedCount) {
        await().atMost(Duration.FOREVER)
                .until(() -> catchAll.findAll(type).size() >= expectedCount);
        List<T> findAll = catchAll.findAll(type);
        assertEquals(expectedCount, findAll.size());
        return findAll;
    }
}
