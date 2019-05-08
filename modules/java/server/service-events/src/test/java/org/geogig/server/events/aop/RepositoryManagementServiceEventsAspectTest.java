package org.geogig.server.events.aop;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.events.model.RepositoryEvent;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Store;
import org.geogig.server.model.User;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class RepositoryManagementServiceEventsAspectTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired CatchAllEventsSubscriber catchAll;

    private @SpyBean RepositoryManagementServiceEventsAspect configAspect;

    private User user1, user2;

    private Store commonStore, store1, store2;

    public @Before void before() {
        commonStore = support.createStore("commonStore");
        store1 = support.createStore("store1");
        store2 = support.createStore("store2");

        user1 = support.createUser("gabe", store1, commonStore);
        user2 = support.createUser("dave", store2, commonStore);
        catchAll.clear();
    }

    public @Test final void testCreateDefaultUserStore() {
        RepoInfo repo1 = support.getRepos().create(user1, "repo1");
        verify(configAspect, times(1)).afterRepositoryCreated(any(), same(repo1));
        assertSame(repo1, catchAll.last(RepositoryEvent.Created.class).getRepository());

        RepoInfo repo2 = support.getRepos().create(user2, "repo1");
        verify(configAspect, times(1)).afterRepositoryCreated(any(), same(repo2));
        assertSame(repo2, catchAll.last(RepositoryEvent.Created.class).getRepository());
    }

    public @Test final void testCreateNonDefaultUserStore() {

        RepoInfo repo1 = support.getRepos().create(user1, "repo1", commonStore.getIdentity(),
                "repo1 descr", true);

        verify(configAspect, times(1)).afterRepositoryCreated(any(), same(repo1));
        assertSame(repo1, catchAll.last(RepositoryEvent.Created.class).getRepository());

        RepoInfo repo2 = support.getRepos().create(user2, "repo1", commonStore.getIdentity(),
                "user2:repo1 descr", true);
        verify(configAspect, times(1)).afterRepositoryCreated(any(), same(repo2));
        assertSame(repo2, catchAll.last(RepositoryEvent.Created.class).getRepository());
    }

    public @Test final void testModify() {
        RepoInfo repo1 = support.getRepos().create(user1, "repo1", commonStore.getIdentity(),
                "repo1 descr", true);
        RepoInfo repo2 = support.getRepos().create(user2, "repo1");

        repo1.setDescription("new description");
        RepoInfo updated = support.getRepos().update(repo1);
        verify(configAspect, times(1)).afterRepositoryModified(any(), same(updated));
        assertSame(updated, catchAll.last(RepositoryEvent.Updated.class).getRepository());

        repo2.setEnabled(false);
        updated = support.getRepos().update(repo2);
        verify(configAspect, times(1)).afterRepositoryModified(any(), same(updated));
        assertSame(updated, catchAll.last(RepositoryEvent.Updated.class).getRepository());
    }

    public @Test final void testDeleteByName() {
        RepoInfo repo1 = support.getRepos().create(user1, "repo1", commonStore.getIdentity(),
                "repo1 descr", true);

        catchAll.clear();
        RepoInfo deleted1 = support.getRepos().remove(user1.getIdentity(), repo1.getIdentity());
        RepositoryEvent.Deleted event1 = catchAll.last(RepositoryEvent.Deleted.class);
        assertSame(deleted1, event1.getRepository());
    }

    public @Test final void testDeleteById() {
        RepoInfo repo1 = support.getRepos().create(user1, "repo1", commonStore.getIdentity(),
                "repo1 descr", true);

        catchAll.clear();
        RepoInfo deleted1 = support.getRepos().remove(repo1.getId());
        RepositoryEvent.Deleted event1 = catchAll.last(RepositoryEvent.Deleted.class);
        assertSame(deleted1, event1.getRepository());
    }
}
