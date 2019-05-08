package org.geogig.server.service.async;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AsyncTasksServiceTestConfiguration.class)
@DataJpaTest
public class AsyncTasksServiceTest {

    private @Autowired AsyncTasksService service;

    public @Test void smokeTest() {
        assertNotNull(service);
    }

}
