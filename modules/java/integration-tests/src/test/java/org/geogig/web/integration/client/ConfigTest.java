package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.UserInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigTest extends AbstractIntegrationTest {

    private Store store;

    private User user;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo("naturalEearth");
    }

    public @Test void testModifyStore() {
        StoreInfo info = store.getInfo();
        info.setDescription("modified description");
        Store updated = store.getClient().stores().modify(info);
        assertNotNull(updated);
        StoreInfo updatedInfo = updated.getInfo();
        assertEquals(info.getDescription(), updatedInfo.getDescription());
    }

    public @Test void testCreateModifyUser() {
        User user = testSupport.createUser("dave", "s3cr3t", "Dave", "dave@example.com",
                store.getIdentity());
        UserInfo pre = user.getInfo();
        user.setFullName("David Blasby");
        user.modify();
        UserInfo post = user.getInfo();
        assertNotSame(pre, post);
        assertEquals("David Blasby", post.getPrivateProfile().getFullName());
    }

    public @Test void testCreateModifyRepo() {
        repo = user.createRepo("naturalEearth2");
        RepositoryInfo pre = repo.getInfo();
        repo.setDescription("updated repo description");
        repo.modify();
        RepositoryInfo post = repo.getInfo();
        assertNotSame(pre, post);
        assertEquals("updated repo description", post.getDescription());
    }
}
