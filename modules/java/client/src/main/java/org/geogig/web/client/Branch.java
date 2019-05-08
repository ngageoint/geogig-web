package org.geogig.web.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.BranchInfo;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.PullArgs;
import org.geogig.web.model.RevertResult;
import org.geogig.web.model.RevisionCommit;

import lombok.NonNull;

public class Branch {

    private RepoBranchesClient client;

    private BranchInfo branch;

    public Branch(RepoBranchesClient client, BranchInfo branch) {
        this.client = client;
        this.branch = branch;
    }

    public BranchInfo getInfo() {
        return branch;
    }

    public Repo getRepo() {
        return client.getRepo();
    }

    public String getName() {
        return branch.getName();
    }

    public String getDescription() {
        return branch.getDescription();
    }

    public RevisionCommit getCommit() {
        return branch.getCommit();
    }

    public AsyncTask<MergeResult> sycWithTrackedBranch() {
        String user = client.getRepo().getOwnerName();
        String repo = client.getRepo().getIdentity();
        String branch = getName();
        UUID txId = client.getRepo().transactionId();
        try {
            PullArgs args = new PullArgs();
            AsyncTaskInfo asyncTaskInfo = client.api.syncBranch(user, repo, branch, txId, args);
            return new AsyncTask<MergeResult>(client.client, asyncTaskInfo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public FeatureServiceClient featureService() {
        return new FeatureServiceClient(this, () -> getRepo().getTransaction().orElse(null));
    }

    /**
     * Creates a new branch off this branch, named after {@literal newBranchName}
     */
    public Branch branch(String newBranchName) {
        return branch(newBranchName, null);
    }

    public Branch branch(String newBranchName, @Nullable String branchDescription) {
        return client.createBranch(getName(), newBranchName, branchDescription);
    }

    /**
     * Creates a pull request from this branch to the specified branch
     * <p>
     * A PR can also be created through {@link Repo#pullRequests() Repo.pullRequests().create(...)},
     * that this method is a shortcut to
     */
    public PullRequest pullRequestTo(@NonNull Branch pullRequestTarget, @NonNull String title,
            String description) {

        // create a new Repo instead of using pullRequestTarget so it uses this Branch's Client
        // credentials
        Repo targetRepo = new Repo(client.getClient(), pullRequestTarget.getRepo().getInfo());
        PullRequest pr = targetRepo.pullRequests().create(this, pullRequestTarget.getName(), title,
                description);
        return pr;
    }

    /**
     * Checks out this branch. The repository MUST have a transaction running.
     * 
     * @return {@literal this}
     */
    public Branch checkout() {
        this.branch = client.checkout(this.getName()).getInfo();
        return this;
    }

    public Branch forceCheckout() {
        this.branch = client.forceCheckout(this.getName()).getInfo();
        return this;
    }

    public boolean conflictsWith(@NonNull Branch other) {
        return client.conflict(getName(), other.getRepo().getOwnerName(),
                other.getRepo().getIdentity(), other.getName());
    }

    public Branch refresh() {
        this.branch = client.get(this.getName()).getInfo();
        return this;
    }

    public String getQualifiedName() {
        return getRepo().getQualifiedName() + "/" + getName();
    }

    public CompletableFuture<Boolean> delete() {
        return CompletableFuture.supplyAsync(() -> {
            client.delete(getName());
            return true;
        });
    }

    public List<RevisionCommit> getCommits() {
        String branch = getName();
        List<RevisionCommit> commits = client.getCommits(branch);
        return commits;
    }

    public List<RevisionCommit> getLayerCommits(@NonNull String layerName) {
        return getLayerCommits(layerName, null);
    }

    public List<RevisionCommit> getLayerCommits(@NonNull String layerName, Integer limit) {
        String branch = getName();
        List<RevisionCommit> commits = client.getCommits(branch, layerName, limit);
        return commits;
    }

    /**
     * Merge head onto this branch
     */
    public AsyncTask<MergeResult> merge(String head) {
        return client.merge(getName(), head);
    }

    public RevisionCommit resetHard(@NonNull String commitIsh) {
        return client.resetHard(getName(), commitIsh);
    }

    public AsyncTask<RevertResult> revert(@NonNull String commitIsh) {
        return client.revert(getName(), commitIsh);
    }
}
