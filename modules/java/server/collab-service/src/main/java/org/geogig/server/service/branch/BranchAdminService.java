package org.geogig.server.service.branch;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.Branch.BranchBuilder;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.MergeResult;
import org.geogig.server.service.rpc.PullArgs;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.user.UserService;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.FindCommonAncestor;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.ResolveCommit;
import org.locationtech.geogig.plumbing.RevParse;
import org.locationtech.geogig.plumbing.UpdateRef;
import org.locationtech.geogig.plumbing.merge.CheckMergeScenarioOp;
import org.locationtech.geogig.plumbing.remotes.RemoteResolve;
import org.locationtech.geogig.porcelain.BranchConfig;
import org.locationtech.geogig.porcelain.BranchConfigOp;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.BranchDeleteOp;
import org.locationtech.geogig.porcelain.BranchResolveOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CheckoutResult;
import org.locationtech.geogig.porcelain.CheckoutResult.Results;
import org.locationtech.geogig.porcelain.NothingToCommitException;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service("BranchAdminService")
@Slf4j
public class BranchAdminService {

    private @Autowired AsyncTasksService async;

    private @Autowired UserService users;

    private @Autowired RepositoryManagementService repos;

    private @Autowired RepositoryRPCService repoCommands;

    private @Autowired TransactionService transactions;

    public Branch createBranch(String user, String repo, String branch, String commitish,
            @Nullable String description, @Nullable UUID txId) {
        Context context = context(user, repo, txId);

        boolean present = context.command(RevParse.class).setRefSpec(commitish).call().isPresent();
        if (!present) {
            throw new NoSuchElementException(commitish + " does not exist");
        }
        context.command(BranchCreateOp.class).setName(branch).setSource(commitish)
                .setDescription(description).call();
        return getBranch(context, branch);
    }

    public void deleteBranch(String user, String repo, String branch, @Nullable UUID txId) {
        Context context = context(user, repo, txId);

        Ref currBranch = context.command(RefParse.class).setName(branch).call().orNull();
        if (null == currBranch) {
            throw new NoSuchElementException(branch + " is not a branch");
        }
        if (!currBranch.getName().startsWith(Ref.HEADS_PREFIX)) {
            throw new IllegalArgumentException(branch + " does not resolve to a branch");
        }

        Ref deletedRef = context.command(BranchDeleteOp.class).setName(branch).call().orNull();
        if (deletedRef == null) {
            throw new NoSuchElementException("Branch " + branch + " does not exist");
        }
        currBranch = context.command(RefParse.class).setName(branch).call().orNull();
        Preconditions.checkState(currBranch == null);
    }

    public Branch getCurrentBranch(String user, String repo, @Nullable UUID txId) {
        return getBranch(user, repo, Ref.HEAD, txId);
    }

    public Branch getBranch(String user, String repo, String branchName, @Nullable UUID txId) {

        Context context = context(user, repo, txId);
        return getBranch(context, branchName);
    }

    private Branch getBranch(Context context, String branchName) {
        try {
            BranchConfig branchInfo = context.command(BranchConfigOp.class).setName(branchName)
                    .get();
            return toBranch(context, branchInfo);
        } catch (IllegalArgumentException e) {
            NoSuchElementException nse = new NoSuchElementException(e.getMessage());
            nse.initCause(e);
            throw nse;
        }
    }

    public Iterator<RevCommit> log(@NonNull String user, @NonNull String repo,
            @NonNull String branchName, @Nullable UUID txId) {

        RepoInfo repositoryInfo = repos.getOrFail(user, repo);
        UUID repoId = repositoryInfo.getId();
        Iterator<RevCommit> commits = repoCommands.getCommits(repoId, branchName, null, null, txId);
        return commits;
    }

    public List<Branch> listBranches(String user, String repo, @Nullable UUID txId) {

        Context context = context(user, repo, txId);
        List<BranchConfig> all = context.command(BranchConfigOp.class).getAll();
        return all.stream().parallel().map(r -> toBranch(context, r)).collect(Collectors.toList());
    }

    private GeogigTransaction transaction(@NonNull String user, @NonNull String repo,
            @NonNull UUID txId) {
        return (GeogigTransaction) context(user, repo, txId);
    }

    private Context context(String user, String repo, UUID txId) {
        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RepositoryConnectionException e) {
            throw new IllegalStateException(e);
        }
        return context;
    }

    public Branch checkout(@NonNull String user, @NonNull String repo, @NonNull String branch,
            @NonNull UUID txId, boolean force) {

        GeogigTransaction transaction = transaction(user, repo, txId);

        final Optional<Ref> currentBranch = transaction.command(BranchResolveOp.class).call();
        if (!currentBranch.isPresent() && !force) {
            throw new IllegalStateException(
                    "Repository is in a dettached state, can't checkout branch " + branch
                            + " if the force parameters isn't true");
        }

        CheckoutResult res = transaction.command(CheckoutOp.class).setForce(force).setSource(branch)
                .call();
        Preconditions.checkState(res.getResult() == Results.CHECKOUT_LOCAL_BRANCH
                || res.getResult() == Results.CHECKOUT_REMOTE_BRANCH);
        return getBranch(transaction, branch);
    }

    private Branch toBranch(Context context, BranchConfig branchStatus) {

        BranchBuilder builder = Branch.builder();
        Optional<RepoInfo> remoteRepo = Optional.empty();
        Ref ref = branchStatus.getBranch();
        ObjectId branchCommitId = ref.getObjectId();
        if (branchStatus.getRemoteName().isPresent()) {
            String remoteName = branchStatus.getRemoteName().get();
            Remote remote = context.command(RemoteResolve.class).setName(remoteName).call()
                    .orNull();
            if (remote != null) {
                URI remoteURI = URI.create(remote.getFetchURL());
                RepoInfo remoteRepoInfo = repos.getByURI(remoteURI)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Could not determine upstream repository"));
                remoteRepo = Optional.of(remoteRepoInfo);
                setCommitDiff(context, branchStatus, builder, remoteRepoInfo);
            }
        } else {
            builder.commitsAhead(Optional.empty()).commitsBehind(Optional.empty());
        }
        RevCommit commit = branchCommitId.isNull() ? null
                : context.objectDatabase().getCommit(branchCommitId);

        return builder//
                .name(ref.localName())//
                .description(branchStatus.getDescription().orElse(null))//
                .commit(commit)//
                .remote(remoteRepo)//
                .remoteName(branchStatus.getRemoteName())//
                .remoteBranch(branchStatus.getRemoteBranch())//
                .build();
    }

    private void setCommitDiff(Context context, BranchConfig branchStatus, BranchBuilder builder,
            RepoInfo remote) {

        Repository remoteRepo;
        try {
            remoteRepo = repos.resolve(remote);
        } catch (RuntimeException e) {
            log.error("Can't open remote {} tracked by branch {}", remote.getIdentity(),
                    branchStatus.getBranch().getName(), e);
            return;
        }

        Ref localBranch = branchStatus.getBranch();
        String remoteBranch = branchStatus.getRemoteBranch().orElse(localBranch.getName());

        Map<String, List<RevCommit>> commitDifferences = commitDifferences(remoteRepo.context(),
                remoteBranch, context, localBranch.getName());

        int commitsBehind = commitDifferences.get("behind").size();
        int commitsAhead = commitDifferences.get("ahead").size();
        builder.commitsBehind(Optional.of(commitsBehind));
        builder.commitsAhead(Optional.of(commitsAhead));
    }

    private RevCommit resolveCommit(Repository repo, String branch) {
        com.google.common.base.Optional<Ref> ref = repo.command(RefParse.class).setName(branch)
                .call();

        if (ref.isPresent()) {
            return repo.getCommit(ref.get().getObjectId());
        }
        throw new IllegalArgumentException(branch + " does not resolve to a branch");
    }

    public Boolean compare(UUID repo1, String branch1, UUID repo2, String branch2) {
        throw new UnsupportedOperationException();
    }

    //@formatter:off
    public boolean conflict(
            @NonNull UUID repo1, 
            @NonNull String branch1, 
            @NonNull UUID repo2,
            @NonNull String branch2) {
            //@formatter:on

        Repository r1 = repos.resolve(repo1);
        Repository r2 = repos.resolve(repo2);
        RevCommit c1 = resolveCommit(r1, branch1);
        RevCommit c2 = resolveCommit(r2, branch2);

        List<RevCommit> commits = Lists.newArrayList(c1, c2);

        Boolean hasConflicts = r1.command(CheckMergeScenarioOp.class).setCommits(commits).call();

        return hasConflicts;
    }

    public Optional<RevCommit> commonAncestor(UUID repo1, String branch1, UUID repo2,
            String branch2) {

        Repository r1 = repos.resolve(repo1);
        Repository r2 = repos.resolve(repo2);
        RevCommit c1 = resolveCommit(r1, branch1);
        RevCommit c2 = resolveCommit(r2, branch2);

        ObjectId commonAncestorId;
        commonAncestorId = r2.command(FindCommonAncestor.class).setLeft(c1).setRight(c2).call()
                .orNull();

        RevCommit commit = null;
        if (commonAncestorId != null) {
            commit = r1.getCommit(commonAncestorId);
        }
        return Optional.ofNullable(commit);
    }

    public RevCommit resetHard(@NonNull String user, @NonNull String repo, @NonNull String branch,
            @NonNull String commitIsh, @Nullable UUID txId) {

        Context context = context(user, repo, txId);
        final Ref branchRef = context.command(BranchResolveOp.class).setName(branch).call()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Branch %s not found", branch)));

        if (branchRef.getName().startsWith(Ref.TAGS_PREFIX)) {
            throw new IllegalArgumentException("A tag's commit cannot be reset");
        }

        RevCommit commit = context.command(ResolveCommit.class).setCommitIsh(commitIsh).call()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("ref-spec %s is not found", commitIsh)));

        Ref updatedRef = context.command(UpdateRef.class).setName(branchRef.getName())
                .setNewValue(commit.getId()).call().get();
        Preconditions.checkState(commit.getId().equals(updatedRef.getObjectId()));
        return commit;
    }

    public Task<RevertResult> revertCommit(//@formatter:off
            @NonNull String user,
            @NonNull String repo, 
            @NonNull String branch, 
            @NonNull String commitIsh,
            @Nullable UUID txId) {//@formatter:on

        final User issuer = users.requireAuthenticatedUser();
        final Transaction transaction;
        final boolean autoCommit;
        try {
            if (txId == null) {
                transaction = transactions.beginTransaction(user, repo);
                autoCommit = true;
            } else {
                transaction = transactions.getTransaction(user, repo, txId);
                autoCommit = false;
            }
        } catch (RepositoryConnectionException e) {
            throw new IllegalStateException(String.format("Unable to create transaction on %s:%s"));
        }

        RevertJob job = RevertJob.builder().transaction(transaction).autoCommit(autoCommit)
                .issuer(issuer).branch(branch).commitIsh(commitIsh).build();

        Task<RevertResult> taskInfo = async.submit(job);
        return taskInfo;
    }

    public Task<MergeResult> sycWithTrackedBranch(//@formatter:off
            @NonNull User caller, 
            @NonNull UUID txId, 
            @NonNull String branch 
            ) {//@formatter:on

        Transaction transaction = transactions.getOrFail(txId);
        GeogigTransaction tx;
        try {
            tx = transactions.resolve(txId);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException();
        }
        Branch targetBranch = getBranch(tx, branch);
        Optional<RepoInfo> remote = targetBranch.getRemote();
        Preconditions.checkArgument(remote.isPresent(), "Branch %s is not tracking a remote branch",
                branch);
        // since remote is present we know commitsAhead/Behing are present too
        if (targetBranch.getCommitsAhead().get() == 0
                && targetBranch.getCommitsBehind().get() == 0) {
            throw new NothingToCommitException(
                    String.format("Branch %s is up to date with remote tracking branch", branch));
        }

        RepoInfo remoteRepo = remote.get();
        User remoteOwner = users.getOrFail(remoteRepo.getOwnerId());
        String remoteBranch = targetBranch.getRemoteBranch().orElse(branch);

        String commitMessage = String.format("Synchronize changes from tracked branch %s:%s:%s",
                remoteOwner.getIdentity(), remoteRepo.getIdentity(), remoteBranch);

        PullArgs args = PullArgs.builder()//
                .commitMessage(commitMessage)//
                .remoteRepo(remoteRepo.getId())//
                .remoteBranch(remoteBranch)//
                .targetRepo(transaction.getRepositoryId())//
                .targetBranch(branch)//
                .build();

        return syncWithRemoteBranch(caller, txId, branch, args);
    }

    /**
     * @param caller the user calling this operation, to record as author of and eventual merge
     *        commit
     * @param txId identifies the target transaction and hence the target repository
     * @param branch the branch where to pull the remote changes into
     * @param remoteRepo the remote repository, if null, the one the target branch is tracking
     * @param remoteBranch the remote branch to pull from, if null, the one the target branch is
     *        tracking
     * @param theirs if {@code true}, resolve possible merge conflicts with the remote's revision
     * @throws IllegalArgumentException if branch is not tracking a remote branch and
     *         {@code remoteRepo/remoteBranch} weren't specified
     * @throws NothingToCommitException if branch is up to date with remote branch
     */
    public Task<MergeResult> syncWithRemoteBranch(//@formatter:off
            @NonNull User caller, 
            @NonNull UUID txId, 
            @NonNull String branch, 
            @NonNull PullArgs args
            ) {//@formatter:on

        Task<MergeResult> task = repoCommands.pull(caller, txId, args);
        return task;
    }

    /*
     * Finds the commit differences ("ahead/behind") between two branches. This is the set
     * difference between them. AHEAD = RIGHT - LEFT, BEHIND = LEFT - RIGHT
     */
    public Map<String, List<RevCommit>> commitDifferences(@NonNull UUID left,
            @NonNull String branch, @NonNull UUID right, @NonNull String branch2) {

        Context leftContext = repos.resolve(left).context();
        Context rightContext = repos.resolve(right).context();
        return commitDifferences(leftContext, branch, rightContext, branch2);
    }

    private Map<String, List<RevCommit>> commitDifferences(@NonNull Context left,
            @NonNull String branch, @NonNull Context right, @NonNull String branch2) {

        CompletableFuture<LinkedHashSet<RevCommit>> leftLog = CompletableFuture
                .supplyAsync(() -> Streams.stream(repoCommands.getCommits(left, branch))
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        CompletableFuture<LinkedHashSet<RevCommit>> rightLog = CompletableFuture
                .supplyAsync(() -> Streams.stream(repoCommands.getCommits(right, branch2))
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        CompletableFuture.allOf(leftLog, rightLog).join();
        LinkedHashSet<RevCommit> commitsLeft = leftLog.join();
        LinkedHashSet<RevCommit> commitsRight = rightLog.join();

        List<RevCommit> behind = new ArrayList<>(Sets.difference(commitsLeft, commitsRight));
        List<RevCommit> ahead = new ArrayList<>(Sets.difference(commitsRight, commitsLeft));

        Map<String, List<RevCommit>> result = ImmutableMap.of("ahead", ahead, "behind", behind);
        return result;
    }
}
