package org.geogig.web.client;

import java.util.List;
import java.util.stream.Collectors;

import org.geogig.web.client.internal.ApiException;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.StoreInfo;

public class Store extends Identity<StoreInfo> {

    Store(Client client, StoreInfo info) {
        super(client, info, () -> info.getId(), () -> info.getIdentity());
    }

    public List<Repo> getRepos() {
        List<RepositoryInfo> repoInfos;
        try {
            repoInfos = client.stores.listStoreRepos(getIdentity());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return repoInfos.stream().map((r) -> new Repo(client, r)).collect(Collectors.toList());
    }

    public List<RepositoryInfo> getOrphanRepos() {
        List<RepositoryInfo> repoInfos;
        try {
            repoInfos = client.stores.listStoreOrphanRepos(getIdentity());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return repoInfos;
    }

    public int getNumRepos() throws ApiException {
        Integer count = client.stores.countStoreRepos(getIdentity());
        return count.intValue();
    }

    public boolean remove() {
        return client.stores().delete(this.getIdentity());
    }

    public void save() {
        Store modified = client.stores().modify(getInfo());
        updateInfo(modified.getInfo());
    }
}
