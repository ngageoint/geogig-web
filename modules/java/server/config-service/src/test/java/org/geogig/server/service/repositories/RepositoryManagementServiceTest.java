package org.geogig.server.service.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import javax.annotation.Nullable;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConfigTestConfiguration.class)
@DataJpaTest
public class RepositoryManagementServiceTest {

    private @Autowired RepositoryManagementService service;

    public @Rule @Autowired ServiceTestSupport support;

    private Store defaultStore, store2, store3;

    private User user1, user2, user3;

    public @Before void before() {
        defaultStore = support.getDefaultStore();
        store2 = support.createStore("another store");
        store3 = support.createStore("yet another store");
        user1 = support.createUser("user1", defaultStore);
        user2 = support.createUser("user2", store2, defaultStore);
        user3 = support.createUser("user3", store3, defaultStore, store2);
    }

    public @Test void testCreate() {
        testCreate(user1, "repo1", null, null, true);
    }

    public @Test void testUpdate() throws Exception {
        RepoInfo repo = create(user1, "repo1", null, null, true);
        repo.setDescription("new description");
        repo.setEnabled(false);
        repo.setDeleted(true);
        repo.setForkedFrom(UUID.randomUUID());
        repo.setIdentity("changedname");
        repo.setOwnerId(user2.getId());
        repo.setStoreId(store3.getId());

        testUpdate(user3, repo);
    }

    private void testCreate(User owner, String repoName, @Nullable String storeName,
            @Nullable String description, boolean enabled) {

        RepoInfo repo = create(owner, repoName, storeName, description, enabled);
        assertEquals(repoName, repo.getIdentity());
        assertEquals(owner.getId(), repo.getOwnerId());
        UUID storeId = storeName == null ? owner.getDefaultStore()
                : support.getStore(storeName).getId();
        assertEquals(storeId, repo.getStoreId());
        assertEquals(description, repo.getDescription());
        assertNull(repo.getForkedFrom());

        assertNotNull(repo.getCreatedAt());
        assertNotNull(repo.getUpdatedAt());
        assertEquals(user2.getIdentity(), repo.getCreatedBy());
        assertEquals(user2.getIdentity(), repo.getModifiedBy());
    }

    private RepoInfo create(User owner, String repoName, String storeName, String description,
            boolean enabled) {
        support.runAs(user2.getIdentity());
        RepoInfo repo = service.create(owner, repoName, storeName, description, enabled);
        assertNotNull(repo);
        assertNotNull(repo.getId());
        return repo;
    }

    private void testUpdate(User updater, RepoInfo param) throws Exception {
        Thread.sleep(200);
        support.runAs(updater.getIdentity());

        RepoInfo repo = service.update(param);

        assertEquals(param.getId(), repo.getId());
        assertEquals(param.getIdentity(), repo.getIdentity());
        assertEquals(param.isEnabled(), repo.isEnabled());
        assertEquals(param.isDeleted(), repo.isDeleted());
        assertEquals(param.getOwnerId(), repo.getOwnerId());
        assertEquals(param.getStoreId(), repo.getStoreId());
        assertEquals(param.getDescription(), repo.getDescription());
        assertEquals(param.getForkedFrom(), repo.getForkedFrom());
        assertEquals(param.getCreatedAt(), repo.getCreatedAt());
        assertEquals(param.getCreatedBy(), repo.getCreatedBy());

        // can't assert this because @DataJpaTest defines @Transactional on the whole test class
        // assertNotEquals(param.getUpdatedAt(), repo.getUpdatedAt());
        // assertEquals(updater.getIdentity(), repo.getModifiedBy());
    }
}
