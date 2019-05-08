package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.RawRepositoryAccessApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.TransactionInfo;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.NonNull;

public class Repo extends Identity<RepositoryInfo> implements Cloneable {

    private @Nullable Transaction currentTransaction;

    public Repo(Client client, RepositoryInfo info) {
        super(client, info, () -> info.getId(), () -> info.getIdentity());
    }

    /**
     * Implementation of {@link java.lang.Object#clone()}, returns a new {@code Repo} instance
     * operating on the same API {@link Client}, useful to run {@link #startTransaction()
     * transactions} on a separate Repo instance.
     * <p>
     * Not to be confused with cloning a repository in the sense of creating a fork of it, which can
     * be achieved through {@link ReposClient#fork}.
     */
    public @Override Repo clone() {
        return new Repo(client, getInfo());
    }
    
    public Repo modify() {
        RepositoryInfo repoInfo = getInfo();
        Repo modified = getClient().repositories().modify(repoInfo);
        super.updateInfo(modified.getInfo());
        return this;
    }

    public String getOwnerName() {
        return getInfo().getOwner().getIdentity();
    }

    public String getDescription() {
        return getInfo().getDescription();
    }

    public void setDescription(String desc) {
        getInfo().setDescription(desc);
    }

    public User getOwner() {
        User owner = client.users().get(getInfo().getOwner().getIdentity());
        return owner;
    }

    /**
     * Starts a transaction managed by this {@code Repo} instance. You can work on a separate
     * transaction against the same repository using another {@code instance}. Until calling
     * {@link #commit()} or {@link #abort()}, all repository queries and operations are isolated to
     * the current transaction.
     * 
     * @throws IllegalStateException is a transaction is already being managed by this repo
     *         instance.
     */
    public Repo startTransaction() {
        setTransaction(() -> client.transactions().startTransaction(getInfo()));
        return this;
    }

    public Optional<Transaction> getTransaction() {
        return Optional.ofNullable(this.currentTransaction);
    }

    public @Nullable UUID transactionId() {
        return isTransactionPresent() ? currentTransaction.getId() : null;
    }

    public boolean isTransactionPresent() {
        return null != currentTransaction;
    }

    public AsyncTask<TransactionInfo> commit() {
        return commit(null, null);
    }

    public AsyncTask<TransactionInfo> commit(String commitMessage) {
        return commit(commitMessage, null);
    }

    /**
     * Commits the current transaction
     * 
     * @param messageTitle
     * @param messageAbstract
     * 
     * @return
     * 
     * @throws IllegalStateException if there's no transaction currently being managed by this repo
     *         instance.
     */
    public AsyncTask<TransactionInfo> commit(String messageTitle, String messageAbstract) {
        checkState(isTransactionPresent());
        AsyncTask<TransactionInfo> commit = currentTransaction.commit(messageTitle,
                messageAbstract);
        currentTransaction = null;
        return commit;
    }

    /**
     * Calls for a commit and waits until the operation finishes on the server.
     * <p>
     * This method is blocking, beware of potential long waits.
     */
    public TransactionInfo commitSync() {
        return commitSync(null, null);
    }

    /**
     * Calls for a commit and waits until the operation finishes on the server.
     * <p>
     * This method is blocking, beware of potential long waits.
     * 
     * @param commitMessageTitle
     * @param commitMessageAbstract
     */
    public TransactionInfo commitSync(@Nullable String commitMessageTitle,
            @Nullable String commitMessageAbstract) {
        AsyncTask<TransactionInfo> task = commit(commitMessageTitle, commitMessageAbstract);
        AsyncTask<TransactionInfo> endState = task.awaitTermination();
        if (endState.isComplete()) {
            return endState.getInfo().getTransaction();
        }
        // TODO handle abnormal asynctask termination
        return endState.getInfo().getTransaction();
    }

    /**
     * Aborts the current transaction;
     * 
     * @throws IllegalStateException if there's no transaction currently being managed by this repo
     *         instance.
     */
    public void abort() {
        checkState(isTransactionPresent());
        currentTransaction.abort();
        currentTransaction = null;
    }

    // /**
    // * Returns a feature service client operating against the currently checked out branch.
    // * <p>
    // * To get a feature service client for a different branch, call {@link
    // Branch#featureService()},
    // * for example: {@code repo.branches().get("mybranch").featureService()}, or
    // * {@link #featureService(String) repo.featureService(branchName)}.
    // *
    // * @return a feature service client operating against the currently checked out branch
    // */
    // public FeatureServiceClient featureService() {
    // Branch currentBranch = branches().getCurrentBranch();
    // return currentBranch.featureService();
    // }
    //
    // public FeatureServiceClient featureService(@NonNull String branchName) {
    // return branches().get(branchName).featureService();
    // }

    public RepoBranchesClient branches() {
        return new RepoBranchesClient(this);
    }

    public RepoPullRequestsClient pullRequests() {
        return new RepoPullRequestsClient(this);
    }

    public Optional<Repo> getForkedFrom() {
        RepositoryInfo originInfo = getInfo().getForkedFrom();
        Repo origin = null;
        if (originInfo != null) {
            String owner = originInfo.getOwner().getIdentity();
            String name = originInfo.getIdentity();
            origin = getClient().repositories().getRepo(owner, name);
        }
        return Optional.ofNullable(origin);
    }

    public List<Repo> getConstellation() {
        return client.repositories().getConstellation(this);
    }

    public String getQualifiedName() {
        return getOwnerName() + "/" + getIdentity();
    }

    public List<RevisionCommit> log() {
        return log("HEAD");
    }

    public List<RevisionCommit> log(@NonNull String head, @Nullable String... pathFilters) {
        return log(head, null, pathFilters);
    }

    public List<RevisionCommit> log(@NonNull String head, @Nullable Integer limit,
            @Nullable String... pathFilters) {
        RawRepositoryAccessApi rawapi = client.rawAccess;
        String user = getOwnerName();
        String repo = getIdentity();
        UUID txId = transactionId();
        List<String> path = pathFilters == null ? null : Arrays.asList(pathFilters);
        try {
            return rawapi.log(user, repo, txId, head, path, limit);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    // diff summary, first parent only
    public List<LayerDiffSummary> getDiffSummary(@NonNull RevisionCommit commit) {
        RawRepositoryAccessApi api = client.rawAccess;
        String user = getOwnerName();
        String repo = getIdentity();
        UUID txId = transactionId();
        List<String> parentIds = commit.getParentIds();
        String left;
        if (parentIds == null || parentIds.isEmpty()) {
            left = Strings.padEnd("", 40, '0');
        } else {
            left = parentIds.get(0);
        }
        String right = commit.getId();
        List<LayerDiffSummary> diffSummary;
        try {
            diffSummary = api.diffSummary(user, repo, left, right, user, repo, txId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return diffSummary;
    }

    public void deleteLayer(@NonNull String layer) {
        Preconditions.checkState(isTransactionPresent());
        Client client = getClient();
        String user = getOwnerName();
        String repo = getIdentity();
        UUID txId = transactionId();
        try {
            client.features.deleteLayer(user, repo, layer, txId);
        } catch (ApiException e) {
            Client.propagate(e);
        }
    }

    /**
     * Resumes a transaction.
     * <p>
     * Transactions are long running isolated repository states that can last indefinitely. A
     * transaction can be resumed at any time if it's still active.
     * 
     * @return
     * 
     * @throws IllegalStateException if a transaction is already active on this repo instance
     */
    public Repo resume(@NonNull UUID transactionId) {
        setTransaction(
                () -> client.transactions().getById(getOwnerName(), getIdentity(), transactionId));

        return this;
    }

    public Repo exitTransaction() {
        this.currentTransaction = null;
        return this;
    }

    private void setTransaction(Supplier<Transaction> tx) {
        checkState(null == currentTransaction,
                "There's a transaction already running on this Repo instance. Either end it or use a separate Repo instance to work on a separate transaction.");
        this.currentTransaction = tx.get();
    }

    public Iterator<ConflictInfo> getConflicts() {
        return getConflicts(null, false);
    }

    public Iterator<ConflictInfo> getConflicts(final @Nullable String pathPrefix,
            final boolean includeDetails) {
        RawRepositoryAccessApi api = getClient().rawAccess;
        Integer page = null;
        Integer pageSize = null;
        List<ConflictInfo> conflicts;
        try {
            conflicts = api.getConflicts(getOwnerName(), getIdentity(), transactionId(), pathPrefix,
                    includeDetails, page, pageSize);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return conflicts.iterator();
    }

    public long getConflictsCount() {
        return getConflictsCount(null);
    }

    public long getConflictsCount(final @Nullable String pathPrefix) {
        RawRepositoryAccessApi api = getClient().rawAccess;
        Long conflictsCount;
        try {
            conflictsCount = api.countConflicts(getOwnerName(), getIdentity(), transactionId(),
                    pathPrefix);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return conflictsCount;
    }

    public AsyncTask<Void> stage() {
        return stage(Collections.emptyList());
    }

    public AsyncTask<Void> stage(@NonNull String... paths) {
        return stage(Arrays.asList(paths));
    }

    public AsyncTask<Void> stage(@NonNull List<String> paths) {
        RawRepositoryAccessApi api = getClient().rawAccess;
        try {
            AsyncTaskInfo stageTask = api.stage(getOwnerName(), getIdentity(), transactionId(),
                    paths);
            return new AsyncTask<Void>(getClient(), stageTask);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

}
