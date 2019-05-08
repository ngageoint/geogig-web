package org.geogig.web.client;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.PullRequestsApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestRequest;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.RequestRequestPatch;
import org.geogig.web.model.RevisionCommit;

import com.google.common.base.Preconditions;

import lombok.NonNull;

public class RepoPullRequestsClient extends AbstractServiceClient<PullRequestsApi> {

    private final Repo repo;

    RepoPullRequestsClient(Repo repo) {
        super(repo.client, repo.client.prs);
        this.repo = repo;
    }

    public Repo getRepo() {
        return repo;
    }

    public Optional<PullRequest> get(int id) {
        PullRequestInfo info = load(id);
        PullRequest pr = info == null ? null : new PullRequest(this, info);
        return Optional.ofNullable(pr);
    }

    @Nullable
    PullRequestInfo load(int id) {
        try {
            String ownerName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            PullRequestInfo prInfo = api.getPullRequest(ownerName, repoName, id);
            return prInfo;
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return null;
        }
    }

    public List<PullRequest> getAllOpen() {
        return getAll(true, false);
    }

    public List<PullRequest> getAllClosed() {
        return getAll(false, true);
    }

    public List<PullRequest> getAll() {
        return getAll(true, true);
    }

    private List<PullRequest> getAll(boolean open, boolean closed) {
        String userName = repo.getOwnerName();
        String repoName = repo.getIdentity();
        try {
            List<PullRequestInfo> prs;
            prs = api.listPullRequests(userName, repoName, open, closed);
            return prs.stream().map((b) -> new PullRequest(this, b)).collect(Collectors.toList());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public PullRequest create(@NonNull Branch fromBranch, @NonNull String targetBranch,
            @NonNull String title, @Nullable String description) {

        PullRequestRequest request = new PullRequestRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTargetBranch(targetBranch);

        request.setSourceRepositryName(fromBranch.getRepo().getIdentity());
        request.setSourceRepositoryOwner(fromBranch.getRepo().getOwnerName());
        request.setSourceRepositoryBranch(fromBranch.getName());

        return create(request);
    }

    public PullRequest create(@NonNull PullRequestRequest request) {
        try {
            String userName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            PullRequestInfo pr = api.createPullRequest(userName, repoName, request);
            return new PullRequest(this, pr);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public PullRequestInfo update(@NonNull Integer prId, String title, String description) {
        Preconditions.checkArgument(title != null || description != null);
        try {
            String userName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            RequestRequestPatch patch = new RequestRequestPatch().title(title)
                    .description(description);
            PullRequestInfo pr = api.updatePullRequest(userName, repoName, prId, patch);
            return pr;
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public PullRequestInfo reOpen(@NonNull Integer prId) {
        try {
            String userName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            RequestRequestPatch patch = new RequestRequestPatch().open(Boolean.TRUE);
            PullRequestInfo pr = api.updatePullRequest(userName, repoName, prId, patch);
            return pr;
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public PullRequestInfo close(@NonNull Integer prId) {
        try {
            String userName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            PullRequestInfo pr = api.closePullRequest(userName, repoName, prId);
            return pr;
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public List<RevisionCommit> getCommits(int prId) {
        List<RevisionCommit> pullRequestCommits;
        try {
            String owner = repo.getOwnerName();
            String reponame = repo.getIdentity();
            pullRequestCommits = api.getPullRequestCommits(owner, reponame, prId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return pullRequestCommits;
    }

    public List<LayerDiffSummary> getDiffSummary(int prId) {
        List<LayerDiffSummary> summary;
        try {
            String owner = repo.getOwnerName();
            String reponame = repo.getIdentity();
            summary = api.getPullRequestDiffSummary(owner, reponame, prId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return summary;
    }

    public GeogigFeatureCollection getDiffFeatures(int prId, @NonNull String layerName) {
        FeatureCollection summary;
        try {
            String owner = repo.getOwnerName();
            String reponame = repo.getIdentity();
            summary = api.getPullRequestDiffFeatures(owner, reponame, prId, layerName);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return (GeogigFeatureCollection) summary;
    }

    public GeogigFeatureCollection getConflictFeatures(int prId, @NonNull String layerName) {
        FeatureCollection summary;
        try {
            String owner = repo.getOwnerName();
            String reponame = repo.getIdentity();
            summary = api.getPullRequestConflictFeatures(owner, reponame, prId, layerName);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return (GeogigFeatureCollection) summary;
    }

    public AsyncTask<PullRequestStatus> merge(int prId) {
        String user = repo.getOwnerName();
        String repoName = repo.getIdentity();
        String commitTitle = null;
        String commitMessage = null;
        AsyncTaskInfo asyncTaskInfo;
        try {
            asyncTaskInfo = api.mergePullRequest(user, repoName, prId, commitTitle, commitMessage);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new AsyncTask<>(getClient(), asyncTaskInfo);
    }

    public PullRequestStatus getStatus(int prId) {
        String user = repo.getOwnerName();
        String repoName = repo.getIdentity();
        PullRequestStatus status;
        try {
            status = api.getPullRequestStatus(user, repoName, prId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return status;
    }
}
