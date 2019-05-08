package org.geogig.server.websockets;

import static org.junit.Assert.fail;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.test.ConfigTestConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

//@formatter:off
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ConfigTestConfiguration.class, EventsConfiguration.class, WebSocketPushEventsConfiguration.class })
@DataJpaTest
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class ActivityLogServiceTest {//@formatter:on

    private @Autowired ActivityLogService logService;

    @Test
    @Ignore
    public final void test() {
        fail("Not yet implemented");
    }

}
