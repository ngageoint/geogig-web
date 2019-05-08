package org.geogig.server.events.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.events.model.TransactionEvent;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.transaction.TransactionsJPAStore;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geogig.repository.Repository;
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
@ContextConfiguration(classes = { TransactionsJPAStore.class, ConfigTestConfiguration.class,
        EventsConfiguration.class })
@DataJpaTest
@Transactional(propagation = Propagation.NEVER) // or other threads don't see db updates
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class TransactionServiceEventsAspectTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired CatchAllEventsSubscriber catchAll;

    private @Autowired TransactionService service;

    private @SpyBean TransactionServiceEventsAspect aspect;

    private User user1, user2;

    private RepoInfo repo1, repo2;

    public @Before void before() {
        catchAll.clear();

        user1 = support.createUser("gabe");
        user2 = support.createUser("dave");
        repo1 = support.createRepo(user1.getIdentity(), "repo1");
        repo2 = support.createRepo(user2.getIdentity(), "repo2");
    }

    public @Test final void testBeginUserNameRepoName() throws Exception {
        catchAll.clear();
        Transaction tx1 = service.beginTransaction(user1.getIdentity(), repo1.getIdentity());
        verify(aspect, times(1)).afterTransactionBegin(any(JoinPoint.class), same(tx1));
        assertEquals(1, catchAll.size());
        TransactionEvent.Created event = catchAll.first(TransactionEvent.Created.class);
        assertSame(tx1, event.getTransaction());

        catchAll.clear();
        Transaction tx2 = service.beginTransaction(user2.getIdentity(), repo2.getIdentity());
        verify(aspect, times(1)).afterTransactionBegin(any(JoinPoint.class), same(tx2));
        assertEquals(1, catchAll.size());
        event = catchAll.first(TransactionEvent.Created.class);
        assertSame(tx2, event.getTransaction());
    }

    public @Test final void testBeginRepoId() throws Exception {
        catchAll.clear();
        Transaction tx1 = service.beginTransaction(repo1.getId());
        verify(aspect, times(1)).afterTransactionBegin(any(JoinPoint.class), same(tx1));
        assertEquals(1, catchAll.size());
        TransactionEvent.Created event = catchAll.first(TransactionEvent.Created.class);
        assertSame(tx1, event.getTransaction());
    }

    public @Test final void testBeginRepoIdCreatorUser() throws Exception {
        catchAll.clear();
        User caller = user2;
        Transaction tx1 = service.beginTransaction(repo1.getId(), caller);
        verify(aspect, times(1)).afterTransactionBegin(any(JoinPoint.class), same(tx1));
        assertEquals(1, catchAll.size());
        TransactionEvent.Created event = catchAll.first(TransactionEvent.Created.class);
        assertSame(tx1, event.getTransaction());
    }

    public @Test final void testDelete() throws Exception {
        Transaction tx1 = service.beginTransaction(user1.getIdentity(), repo1.getIdentity());
        Transaction tx2 = service.beginTransaction(user2.getIdentity(), repo2.getIdentity());

        catchAll.clear();
        service.deleteTransaction(tx1.getId());
        verify(aspect, times(1)).afterTransactionDeleted(any(JoinPoint.class), eq(tx1));
        TransactionEvent.Deleted event = catchAll.first(TransactionEvent.Deleted.class);
        assertEquals(tx1, event.getTransaction());

        catchAll.clear();
        service.deleteTransaction(tx2.getId());
        verify(aspect, times(1)).afterTransactionDeleted(any(JoinPoint.class), eq(tx2));
        event = catchAll.first(TransactionEvent.Deleted.class);
        assertEquals(tx2, event.getTransaction());
    }

    public @Test final void testDeleteNonExistentProducesNoEvent() throws Exception {
        catchAll.clear();
        service.deleteTransaction(UUID.randomUUID());
        verify(aspect, times(1)).afterTransactionDeleted(any(JoinPoint.class), isNull());
        assertEquals(0, catchAll.size());
    }

    public @Test final void testCommitSuccess() throws Throwable {
        Transaction tx1 = service.beginTransaction(user1.getIdentity(), repo1.getIdentity());
        Repository repository = support.getRepository(repo1);
        new TestData(repository).resumeTransaction(tx1.getId()).loadDefaultData();

        catchAll.clear();
        CompletableFuture<Transaction> future = service.commit(user2, tx1, "");
        Transaction result = future.get();
        assertNotNull(result);
        verify(aspect, times(1)).aroundTransactionCommit(any(ProceedingJoinPoint.class));
        assertEquals(1, catchAll.size());
        TransactionEvent.Committed event = catchAll.first(TransactionEvent.Committed.class);
        assertTrue(event.isSuccess());
        assertEquals(user2.getId(), event.getCaller().get().getId());
        assertEquals(tx1.getId(), event.getTransaction().getId());
        assertEquals(Transaction.Status.COMMITTED, event.getTransaction().getStatus());
    }

    public @Test final void testCommitFailed() throws Throwable {

        Repository repository = support.getRepository(repo1);
        TestData testData = new TestData(repository).loadDefaultData();

        Transaction tx = service.beginTransaction(user1.getIdentity(), repo1.getIdentity());
        // produce a conflict
        SimpleFeature f1 = TestData.clone(TestData.point1);
        SimpleFeature f2 = TestData.clone(TestData.point1);
        f1.setAttribute("sp", "changed outside transaction");
        f2.setAttribute("sp", "changed inside transaction");

        testData.insert(f1).add().commit("outside tx change");
        testData.resumeTransaction(tx.getId()).insert(f2).add().commit("inside tx change");

        catchAll.clear();
        CompletableFuture<Transaction> future = service.commit(user2, tx, "");
        try {
            future.get();
            fail("Expected ExecutionException due to merge conflict");
        } catch (ExecutionException expected) {
            assertThat(expected.getMessage(), Matchers.containsString("CONFLICT"));
        }
        verify(aspect, times(1)).aroundTransactionCommit(any(ProceedingJoinPoint.class));
        assertEquals(1, catchAll.size());
        TransactionEvent.Committed event = catchAll.first(TransactionEvent.Committed.class);
        assertFalse(event.isSuccess());
        assertEquals(user2.getId(), event.getCaller().get().getId());
        assertEquals(tx.getId(), event.getTransaction().getId());
        assertEquals(Transaction.Status.OPEN, event.getTransaction().getStatus());
    }

    public @Test final void testAborted() throws Throwable {
        Transaction tx1 = service.beginTransaction(user1.getIdentity(), repo1.getIdentity());
        Repository repository = support.getRepository(repo1);
        new TestData(repository).resumeTransaction(tx1.getId()).loadDefaultData();

        catchAll.clear();
        Transaction result = service.abortTransaction(tx1.getId(), user2);
        assertNotNull(result);
        verify(aspect, times(1)).afterTransactionAborted(any(JoinPoint.class), isNotNull());
        assertEquals(1, catchAll.size());
        TransactionEvent.Aborted event = catchAll.first(TransactionEvent.Aborted.class);
        assertEquals(tx1.getId(), event.getTransaction().getId());
        assertEquals(Transaction.Status.ABORTED, event.getTransaction().getStatus());
    }
}
