package org.geogig.server.stats.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geogig.server.events.EventsConfiguration;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.RepoStats;
import org.geogig.server.model.Store;
import org.geogig.server.model.User;
import org.geogig.server.stats.StatsConfiguration;
import org.geogig.server.test.ConfigTestConfiguration;
import org.geogig.server.test.ServiceTestSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ConfigTestConfiguration.class, EventsConfiguration.class,
        StatsConfiguration.class })
@DataJpaTest
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class RepositoryStatsServiceTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired RepositoryStatsService service;

    private User user1, user2;

    public @Before void init() {
        Store store = support.getDefaultStore();
        user1 = support.createUser("gabe", store);
        user2 = support.createUser("dave", store);
    }

    public @Test final void onCreated() {
        support.runAs("gabe");
        RepoInfo repo1 = support.createRepo(user1, "repo1");
        RepoStats stats = service.get(repo1).orElse(null);
        assertNotNull(stats);
        assertNotNull(stats.getCreatedAt());

        support.runAs("dave");
        RepoInfo repo2 = support.createRepo(user2, "repo1");
        assertTrue(service.get(repo2).isPresent());
        assertNotNull(service.get(repo2).get().getCreatedAt());
    }

    public @Test final void onDeleted() {
        RepoInfo repo1 = support.createRepo(user1, "repo1");
        assertTrue(service.get(repo1).isPresent());
        assertNotNull(service.get(repo1).get().getCreatedAt());
        support.getRepos().remove(repo1.getId());
        assertFalse(service.get(repo1).isPresent());
    }
}
