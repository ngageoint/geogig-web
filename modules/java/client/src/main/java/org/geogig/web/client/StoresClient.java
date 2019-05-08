package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.RepositoryStoresApi;
import org.geogig.web.model.StoreInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

public class StoresClient extends AbstractServiceClient<RepositoryStoresApi> {

    StoresClient(Client client) {
        super(client, client.stores);
    }

    public List<Store> getAll() {
        List<StoreInfo> infos;
        try {
            infos = api.getStores();
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        List<Store> clients = infos.stream().map((s) -> new Store(client, s))
                .collect(Collectors.toList());
        return clients;
    }

    public Optional<Store> get(String storeName) {
        checkNotNull(storeName);
        try {
            StoreInfo info = api.getStore(storeName);
            return Optional.of(new Store(client, info));
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return Optional.empty();
        }
    }

    public Store modify(StoreInfo info) {
        try {
            StoreInfo updated = api.modifyStore(info);
            return new Store(client, updated);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public Store create(StoreInfo info) {
        StoreInfo fullInfo;
        try {
            fullInfo = api.createStore(info);
        } catch (ApiException e) {
            Map<String, List<String>> responseHeaders = e.getResponseHeaders();
            String msg = e.getMessage();
            if (responseHeaders != null && responseHeaders.containsKey("x-geogig-error-message")) {
                List<String> list = responseHeaders.get("x-geogig-error-message");
                msg = Joiner.on(", ").join(list);
            } else if (responseHeaders != null) {
                List<String> list = responseHeaders.get("Content-Type");
                if (list.size() == 1 && list.get(0).startsWith("application/json")) {
                    ObjectMapper mapper = client.getObjectMapper();
                    String responseBody = e.getResponseBody();
                    try {
                        JsonNode response = mapper.reader().readTree(responseBody);
                        JsonNode message = response.get("message");
                        if (message != null) {
                            msg = message.textValue();
                        }
                    } catch (IOException e1) {
                        msg = e.getMessage();
                    }
                }
            }

            throw new IllegalStateException(msg);
        }
        return new Store(client, fullInfo);
    }

    public boolean delete(String storeName) {
        try {
            api.removeStore(storeName);
            return true;
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return false;
        }
    }

}
