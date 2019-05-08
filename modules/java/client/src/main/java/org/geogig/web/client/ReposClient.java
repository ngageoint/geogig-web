package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.RepositoryManagementApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RepositoryInfo;

import lombok.NonNull;

public class ReposClient extends AbstractServiceClient<RepositoryManagementApi> {

    ReposClient(Client client) {
        super(client, client.repositories);
    }

    public Repo create(String owner, String repoName) {
        return create(owner, repoName, null, null);
    }

    public Repo create(String owner, String repoName, @Nullable String targetStore) {
        return create(owner, repoName, targetStore, null);
    }

    public Repo create(String owner, String repoName, @Nullable String targetStore,
            @Nullable String description) {
        checkNotNull(owner);
        checkNotNull(repoName);
        RepositoryInfo createdRepo;
        try {
            IdentifiedObject md = new IdentifiedObject().description(description);
            createdRepo = api.createRepository(owner, repoName, targetStore, md);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new Repo(client, createdRepo);
    }

    public boolean delete(String user, String repo) {
        try {
            api.deleteRepository(user, repo);
            return true;
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return false;
        }
    }

    public List<Repo> getUserRepositories(String user) {
        List<RepositoryInfo> infos;
        try {
            infos = api.listUserRepositories(user);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        List<Repo> clients = toRepoList(infos);
        return clients;
    }

    private List<Repo> toRepoList(List<RepositoryInfo> infos) {
        List<Repo> clients = infos.stream().map((u) -> new Repo(client, u))
                .collect(Collectors.toList());
        return clients;
    }

    public Repo getRepo(String ownerName, String repoName) {
        RepositoryInfo info;
        try {
            info = api.getRepository(ownerName, repoName);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new Repo(client, info);
    }

    public AsyncTask<RepositoryInfo> fork(Repo origin) {
        return fork(origin, null, null);
    }

    public AsyncTask<RepositoryInfo> fork(Repo origin, @Nullable String targetRepoName) {
        return fork(origin, targetRepoName, null);
    }

    public AsyncTask<RepositoryInfo> fork(Repo origin, @Nullable String targetRepoName,
            @Nullable String targetRepoStoreName) {

        String originOwner = origin.getOwnerName();
        String originRepo = origin.getIdentity();
        try {
            AsyncTaskInfo taskInfo;
            taskInfo = api.forkRepository(originOwner, originRepo, targetRepoName,
                    targetRepoStoreName);
            return new AsyncTask<>(client, taskInfo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public List<Repo> getConstellation(@NonNull Repo of) {
        List<RepositoryInfo> constellation;
        try {
            constellation = api.getConstellation(of.getOwnerName(), of.getIdentity());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return toRepoList(constellation);
    }

    public Repo modify(@NonNull RepositoryInfo repo) {
        checkNotNull(repo.getId());
        checkNotNull(repo.getIdentity());
        checkNotNull(repo.getStore());
        checkNotNull(repo.getOwner());
        try {
            RepositoryInfo modified = api.modifyRepositoryById(repo);
            return new Repo(client, modified);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }
}
