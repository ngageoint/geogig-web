package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class AbstractServiceClient<API> {

    protected final Client client;

    protected final API api;

    AbstractServiceClient(Client client, API api) {
        checkNotNull(client);
        checkNotNull(api);
        this.client = client;
        this.api = api;
    }

    public Client getClient() {
        return client;
    }
}
