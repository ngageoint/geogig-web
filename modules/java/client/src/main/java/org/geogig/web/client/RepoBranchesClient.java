package org.geogig.web.client;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.BranchesApi;
import org.geogig.web.client.internal.RawRepositoryAccessApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.BranchInfo;
import org.geogig.web.model.MergeRequest;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.RevertResult;
import org.geogig.web.model.RevisionCommit;

import lombok.NonNull;

public class RepoBranchesClient extends AbstractServiceClient<BranchesApi> {

    private final Repo repo;

    RepoBranchesClient(Repo repo) {
        super(repo.client, repo.client.branches);
        this.repo = repo;
    }

    public Repo getRepo() {
        return repo;
    }

    public Optional<Branch> tryGet(String name) {
        try {
            return Optional.of(get(name));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public Branch get(String name) {
        try {
            String ownerName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            UUID transactionId = repo.transactionId();
            BranchInfo branch = api.getBranch(ownerName, repoName, name, transactionId);
            return new Branch(this, branch);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public @Nullable Branch getCurrentBranch() {
        Branch currentBranch = null;
        try {

            String ownerName = repo.getOwnerName();
            String repoName = repo.getIdentity();
            UUID transactionId = repo.transactionId();
            BranchInfo branch = api.getCurrentBranch(ownerName, repoName, transactionId);
            currentBranch = new Branch(this, branch);
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
        }
        return currentBranch;
    }

    public Optional<Branch> currentBranch() {
        return Optional.ofNullable(getCurrentBranch());
    }

    public List<Branch> getAll() {
        UUID transactionId = repo.transactionId();
        String userName = repo.getOwnerName();
        String repoName = repo.getIdentity();
        try {
            List<BranchInfo> branches;
            branches = api.listBranches(userName, repoName, transactionId);
            return branches.stream().map((b) -> new Branch(this, b)).collect(Collectors.toList());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public void delete(String branchName) {
        UUID transactionId = repo.transactionId();
        String userName = repo.getOwnerName();
        String repoName = repo.getIdentity();
        try {
            api.deleteBranch(userName, repoName, branchName, transactionId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public Branch createBranch(String originBranch, String newBranch,
            @Nullable String branchDescription) {

        String userName = repo.getOwnerName();
        String repoName = repo.getIdentity();
        UUID txId = repo.transactionId();
        try {
            BranchInfo branch = api.createBranch(userName, repoName, newBranch, originBranch, txId,
                    branchDescription);
            return new Branch(this, branch);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public Branch checkout(String branchName) {
        return checkout(branchName, false);
    }

    public Branch forceCheckout(String branchName) {
        return checkout(branchName, true);
    }

    private Branch checkout(String branchName, boolean force) {
        String userName = repo.getOwnerName();
        String repoName = repo.getIdentity();
        UUID txId = repo.transactionId();
        try {
            BranchInfo branch = api.checkout(userName, repoName, branchName, txId, force);
            return new Branch(this, branch);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public boolean conflict(@NonNull String localBranch, @NonNull String user,
            @NonNull String targetRepo, @NonNull String targetBranch) {
        try {
            return api.conflictsWith(repo.getOwnerName(), repo.getIdentity(), localBranch, user,
                    targetRepo, targetBranch);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public List<RevisionCommit> getCommits(@NonNull String branch) {
        List<RevisionCommit> commits;
        try {
            String u = repo.getOwnerName();
            String r = repo.getIdentity();
            UUID tx = repo.transactionId();
            commits = api.getBranchCommits(u, r, branch, tx);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return commits;
    }

    public List<RevisionCommit> getCommits(@NonNull String branch, @NonNull String layerName) {
        return repo.log(branch, layerName);
    }

    public List<RevisionCommit> getCommits(@NonNull String branch, @NonNull String layerName,
            Integer limit) {
        return repo.log(branch, limit, layerName);
    }

    /**
     * Merge the commit {@code head} points to onto the commit {@code base} points out to, with
     * {@code base} being a branch name and {@code head} any refSpec that resolves to a commit.
     * 
     * @param base the target branch name
     * @param head a refspec that resolves to a commit, that's to be merged onto base
     * @return the asynchronous result of the operation
     */
    public AsyncTask<MergeResult> merge(@NonNull String base, @NonNull String head) {
        return merge(base, head, false, null);
    }

    /**
     * Make a no fast-forward merge of the commit {@code head} points to onto the commit
     * {@code base} points out to, with {@code base} being a branch name and {@code head} any
     * refSpec that resolves to a commit.
     * 
     * @param base the target branch name
     * @param head a refspec that resolves to a commit, that's to be merged onto base
     * @return the asynchronous result of the operation
     */
    public AsyncTask<MergeResult> mergeNoFF(@NonNull String base, @NonNull String head,
            @NonNull String commitMessage) {
        return merge(base, head, true, commitMessage);
    }

    private AsyncTask<MergeResult> merge(@NonNull String base, @NonNull String head, boolean noFF,
            String commitMessage) {
        RawRepositoryAccessApi rpc = repo.client.rawAccess;
        String user = this.repo.getOwnerName();
        String repo = this.repo.getIdentity();
        UUID tx = this.repo.transactionId();
        MergeRequest request = new MergeRequest();
        request.setBase(base);
        request.setHead(head);
        request.setCommitMessage(commitMessage);
        request.setNoFf(noFF);

        AsyncTaskInfo taskInfo;
        try {
            taskInfo = rpc.merge(user, repo, tx, request);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new AsyncTask<MergeResult>(super.client, taskInfo);
    }

    public RevisionCommit resetHard(@NonNull String branch, @NonNull String commitIsh) {
        BranchesApi api = super.api;
        String user = this.repo.getOwnerName();
        String repoName = this.repo.getIdentity();
        UUID tx = this.repo.transactionId();
        RevisionCommit newTip;
        try {
            newTip = api.resetHard(user, repoName, branch, commitIsh, tx);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return newTip;
    }

    public AsyncTask<RevertResult> revert(@NonNull String branch, @NonNull String commitIsh) {
        BranchesApi api = super.api;
        String user = this.repo.getOwnerName();
        String repoName = this.repo.getIdentity();
        UUID tx = this.repo.transactionId();
        AsyncTaskInfo taskInfo;
        try {
            taskInfo = api.revertCommit(user, repoName, branch, commitIsh, tx);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new AsyncTask<RevertResult>(super.client, taskInfo);
    }

}
