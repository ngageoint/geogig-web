package org.geogig.web.client;

import java.util.List;

import org.geogig.web.client.internal.ApiException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class StoresClientTest {

    private String baseURL = "http://localhost:8080/v2";

    private Client client;

    private StoresClient stores;

    public @Before void before() {
        client = new Client(baseURL);
        stores = client.stores();
    }

    public @Test void test() throws ApiException {
        List<Store> list = stores.getAll();
        System.err.print(client.getServerVersion());
        System.err.print(list);
    }

}
