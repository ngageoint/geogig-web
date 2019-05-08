package org.geogig.server.events.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.events.model.StoreEvent;
import org.geogig.server.model.Store;
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
public class StoreServiceEventsAspectTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired CatchAllEventsSubscriber catchAll;

    private @SpyBean StoreServiceEventsAspect configAspect;

    public @Before void before() {
        catchAll.clear();
    }

    public @Test final void testCreate() {
        Store store = support.createStore("store1");
        assertNotNull(store);
        verify(configAspect, times(1)).afterStoreCreated(any(), same(store));
        assertEquals(1, catchAll.size());
        StoreEvent.Created event = catchAll.first(StoreEvent.Created.class);
        assertSame(store, event.getStore());
    }

    public @Test final void testModify() {
        Store store = support.createStore("store1");
        store.setDescription("modified description");
        store.setEnabled(false);
        Store modified = support.getStores().modify(store);
        assertNotNull(modified);
        verify(configAspect, times(1)).afterStoreModified(any(), same(modified));

        StoreEvent.Updated event = catchAll.last(StoreEvent.Updated.class);
        assertSame(modified, event.getStore());
    }

    public @Test final void testRemoveById() {
        Store store = support.createStore("store1");
        Store deleted = support.getStores().removeById(store.getId());
        assertNotNull(deleted);
        verify(configAspect, times(1)).afterStoreDeleted(any(), same(deleted));
        StoreEvent.Deleted event = catchAll.last(StoreEvent.Deleted.class);
        assertSame(deleted, event.getStore());
    }

    public @Test final void testRemoveByName() {
        Store store = support.createStore("store1");
        Store deleted = support.getStores().removeByName(store.getIdentity());
        assertNotNull(deleted);
        verify(configAspect, times(1)).afterStoreDeleted(any(), same(deleted));
        StoreEvent.Deleted event = catchAll.last(StoreEvent.Deleted.class);
        assertSame(deleted, event.getStore());
    }
}
