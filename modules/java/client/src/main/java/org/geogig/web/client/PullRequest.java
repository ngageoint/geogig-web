package org.geogig.web.client;

import java.util.List;
import java.util.NoSuchElementException;

import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.RevisionCommit;

import lombok.NonNull;

public class PullRequest {

    private RepoPullRequestsClient client;

    private PullRequestInfo prInfo;

    public PullRequest(RepoPullRequestsClient pullRequestsClient, PullRequestInfo prInfo) {
        this.client = pullRequestsClient;
        this.prInfo = prInfo;
    }

    /**
     * @return the repository the PR was created for
     */
    public Repo getTargetRepo() {
        return client.getRepo();
    }

    /**
     * @return the repository where the PR was originated
     */
    public Repo getSourceRepo() {
        Client apiClient = client.getClient();
        String sourceRepoOwner = getInfo().getSourceRepo().getOwner().getIdentity();
        String sourceRepoName = getInfo().getSourceRepo().getIdentity();
        ReposClient repositories = apiClient.repositories();
        Repo sourceRepo = repositories.getRepo(sourceRepoOwner, sourceRepoName);
        return sourceRepo;
    }

    public PullRequestInfo getInfo() {
        return prInfo;
    }

    public PullRequestStatus status() {
        return client.getStatus(getId());
    }

    public PullRequest refresh() {
        PullRequestInfo info = client.load(prInfo.getId());
        if (info == null) {
            throw new NoSuchElementException();
        }
        this.prInfo = info;
        return this;
    }

    public RevisionCommit sourceCommit() {
        Repo sourceRepo = getSourceRepo();
        String sourceBranch = getInfo().getSourceBranch();
        Branch branch = sourceRepo.branches().get(sourceBranch);
        return branch.getCommit();
    }

    public Integer getId() {
        return getInfo().getId();
    }

    public String getTitle() {
        return getInfo().getTitle();
    }

    public String getDescription() {
        return getInfo().getDescription();
    }

    public List<RevisionCommit> getCommits() {
        return client.getCommits(getId());
    }

    public List<LayerDiffSummary> getDiffSummary() {
        return client.getDiffSummary(getId());
    }

    public Branch getSourceBranch() {
        RepoBranchesClient branches = getSourceRepo().branches();
        String sourceBranch = getInfo().getSourceBranch();
        Branch branch = branches.get(sourceBranch);
        return branch;
    }

    public Branch getTargetBranch() {
        return getTargetRepo().branches().get(getInfo().getTargetBranch());
    }

    public AsyncTask<PullRequestStatus> merge() {
        return client.merge(getId());
    }

    public void close() {
        prInfo = client.close(getId());
    }

    public void reOpen() {
        prInfo = client.reOpen(getId());
    }

    public void setTitle(@NonNull String title) {
        prInfo = client.update(getId(), title, null);
    }

    public void setDescription(@NonNull String description) {
        prInfo = client.update(getId(), null, description);
    }

    public GeogigFeatureCollection getDiffFeatures(@NonNull String layerName) {
        return client.getDiffFeatures(getId(), layerName);
    }

    public GeogigFeatureCollection getConflictFeatures(@NonNull String layerName) {
        return client.getConflictFeatures(getId(), layerName);
    }
}
