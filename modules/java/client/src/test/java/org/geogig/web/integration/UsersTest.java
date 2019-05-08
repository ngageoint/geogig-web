package org.geogig.web.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geogig.web.client.Client;
import org.geogig.web.client.User;
import org.geogig.web.client.internal.ApiException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore
public class UsersTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TestSupport support = new TestSupport();

    private Client client;

    private Client login() {
        User authenticated = client.login("gabe", "geogig");
        assertNotNull(authenticated);
        return client;
    }

    public @Before void before() {
        client = support.newClient();
    }

    public @Test void getSelfUnauthenticated() throws ApiException {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Authentication required");
        client.users().getSelf();
    }

    public @Test void getSelf() throws ApiException {
        User self = login().getSelf();
        assertNotNull(self);
        assertEquals("gabe", self.getIdentity());
    }
}
