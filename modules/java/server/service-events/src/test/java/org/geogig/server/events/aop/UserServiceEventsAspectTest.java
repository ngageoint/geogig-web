package org.geogig.server.events.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.events.model.UserEvent;
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
public class UserServiceEventsAspectTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired CatchAllEventsSubscriber catchAll;

    private @SpyBean UserServiceEventsAspect configAspect;

    private Store store1, store2;

    public @Before void before() {
        store1 = support.createStore("store1");
        store2 = support.createStore("store2");
        catchAll.clear();
    }

    public @Test final void testCreate() {
        UserEvent.Created event;
        User user1 = support.createUser("gabe", store1, store2);
        assertEquals(1, catchAll.size());
        verify(configAspect, times(1)).afterUserCreated(any(), same(user1));
        event = catchAll.last(UserEvent.Created.class);
        assertSame(user1, event.getUser());

        User user2 = support.createUser("dave", store2);
        verify(configAspect, times(1)).afterUserCreated(any(), same(user2));
        assertEquals(2, catchAll.size());
        event = catchAll.last(UserEvent.Created.class);
        assertSame(user2, event.getUser());
    }

    public @Test final void testModify() {

        User user = support.createUser("gabe", store1, store2);
        user.setAdditionalStores(Collections.emptySet());
        user.setEmailAddress("my.new.address@email.com");

        catchAll.clear();
        User modified = support.getUsers().modify(user);
        assertNotNull(modified);
        verify(configAspect, times(1)).afterUserModified(any(), same(modified));

        UserEvent.Updated event = catchAll.last(UserEvent.Updated.class);
        assertSame(modified, event.getUser());
    }

    public @Test final void testDeleteByName() {
        User user1 = support.createUser("gabe", store1, store2);
        User user2 = support.createUser("dave", store2, store1);

        catchAll.clear();
        User deleted1 = support.getUsers().deleteByName(user1.getIdentity());
        verify(configAspect, times(1)).afterUserDeleted(any(), same(deleted1));
        UserEvent.Deleted event1 = catchAll.last(UserEvent.Deleted.class);
        assertSame(deleted1, event1.getUser());

        User deleted2 = support.getUsers().deleteByName(user2.getIdentity());
        verify(configAspect, times(1)).afterUserDeleted(any(), same(deleted2));
        UserEvent.Deleted event2 = catchAll.last(UserEvent.Deleted.class);
        assertSame(deleted2, event2.getUser());
    }
}
