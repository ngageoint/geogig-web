package org.geogig.server.service.pr;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.commands.pr.PR;
import org.geogig.commands.pr.PRStatus;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequest.Status;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTaskProgress;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.ProvidedFutureJob;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.branch.DiffSummary;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.user.UserService;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("PullRequestService")
public class PullRequestService {

    @Autowired
    UserService users;

    private @Autowired RepositoryManagementService repositories;

    private @Autowired RepositoryRPCService repoRpc;

    private @Autowired PullRequestStore store;

    private @Autowired BranchAdminService branches;

    private @Autowired AsyncTasksService async;

    private @Autowired TransactionService transactions;

    private PullRequestWorkerService prWorker;

    public @Autowired PullRequestService(PullRequestWorkerService prWorker) {
        this.prWorker = prWorker;
    }

    public @NonNull PullRequest create(@NonNull PullRequestRequest request) {
        final User caller = users.requireAuthenticatedUser();
        // TODO: verify caller has permissions to create the requested pr
        final RepoInfo targetRepo = repositories.getOrFail(request.getTargetRepo());
        final PullRequest created = createServicePR(request);
        CompletableFuture<PR> repoPRFuture = prWorker.init(created);
        try {
            PR repoPR = repoPRFuture.get();
            checkNotNull(repoPR);
            UUID transactionId = repoPR.getTransactionId();
            UUID creatorId = request.getIssuerUser();
            transactions.createTransaction(targetRepo.getOwnerId(), targetRepo.getId(), creatorId,
                    transactionId);
        } catch (Exception e) {
            store.remove(created.getRepositoryId(), created.getId());
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }

        // run the test merge and return immediately
        checkMergeable(created, request.getIssuerUser());

        return created;
    }

    public CompletableFuture<List<PullRequest>> findPullRequestsAffectedByBranch(
            @NonNull URI repositoryURI, @NonNull Ref ref) {

        return prWorker.findPullRequestsAffectedByBranch(repositoryURI, ref);
    }

    public CompletableFuture<PullRequestStatus> checkMergeableAbortIfRunning(
            @NonNull PullRequest pr) {
        return prWorker.prepareAbortIfRunning(pr);
    }

    public CompletableFuture<PullRequestStatus> checkMergeable(final @NonNull PullRequest pr,
            @NonNull UUID issuerId) {
        CompletableFuture<PullRequestStatus> testMerge = prWorker.prepare(pr, issuerId);
        return testMerge;
    }

    private PullRequest createServicePR(PullRequestRequest request) {
        final UUID issuerRepo = request.getIssuerRepo();
        final UUID targetRepo = request.getTargetRepo();
        final String issuerBranch = request.getIssuerBranch();
        final String targetBranch = request.getTargetBranch();
        checkArgument(!(issuerRepo.equals(targetRepo) && issuerBranch.equals(targetBranch)),
                "can't create a request to itself");

        Set<RepoInfo> issuersConstellation = repositories.getConstellationOf(issuerRepo);
        issuersConstellation.stream().filter((r) -> r.getId().equals(targetRepo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Target repository is not in source repository's constellation"));

        PullRequest newPr = asPullRequest(request);
        PullRequest created = store.create(newPr);
        return created;
    }

    private PullRequest asPullRequest(PullRequestRequest request) {
        PullRequest pr = new PullRequest();
        pr.setRepositoryId(request.getTargetRepo());
        pr.setTargetBranch(request.getTargetBranch());
        pr.setIssuerRepo(request.getIssuerRepo());
        pr.setIssuerUser(request.getIssuerUser());
        pr.setIssuerBranch(request.getIssuerBranch());
        pr.setTitle(request.getTitle());
        pr.setDescription(request.getDescription());
        pr.setStatus(Status.OPEN);
        return pr;
    }

    public Optional<PullRequest> get(UUID repoId, int id) {
        return store.get(repoId, id);
    }

    public PullRequest getOrFail(UUID repoId, int id) {
        return get(repoId, id).orElseThrow(
                () -> new NoSuchElementException(String.format("pr %d does not exist", id)));
    }

    public Iterable<PullRequest> getByRepository(UUID repoId, boolean open, boolean closed) {
        Iterable<PullRequest> repoPulls = store.getByTargetRepository(repoId);
        return Iterables.filter(repoPulls, (pr) -> filter(pr, open, closed));
    }

    private boolean filter(PullRequest pr, boolean open, boolean closed) {
        Status status = pr.getStatus();
        switch (status) {
        case CLOSED:
        case MERGED:
            return closed;
        case OPEN:
            return open;
        default:
            throw new IllegalStateException("Unknown status: " + status);
        }
    }

    //@formatter:off
    public Iterable<PullRequest> listPullRequests(
            String user,
            String repo,
            boolean open,
            boolean closed) {
        //@formatter:on

        final RepoInfo targetRepo = repositories.getByName(user, repo)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("repository %s:%s not found", user, repo)));

        Iterable<PullRequest> reqs = getByRepository(targetRepo.getId(), open, closed);

        return reqs;
    }

    //@formatter:off
    public Optional<PullRequest> getPullRequest(
            String user,
            String repo,
            int prId) {
        //@formatter:on

        final RepoInfo targetRepo = repositories.getOrFail(user, repo);
        Optional<PullRequest> pr = get(targetRepo.getId(), prId);
        return pr;
    }

    public PullRequest getOrFail(@NonNull String user, @NonNull String repo, int prId)
            throws NoSuchElementException {
        return getPullRequest(user, repo, prId).orElseThrow(() -> new NoSuchElementException(
                String.format("pr %d does not exist at %s/%s", prId, user, repo)));
    }

    //@formatter:off
    public Iterator<RevCommit> getCommits(
            @NonNull String user,
            @NonNull String repo,
            final int prId) {
        //@formatter:on

        final PullRequest pr = getOrFail(user, repo, prId);

        UUID targetRepo = pr.getRepositoryId();
        String targetBranch = pr.getTargetBranch();
        UUID issuerRepo = pr.getIssuerRepo();
        String issuerBranch = pr.getIssuerBranch();

        final RevCommit commonAncestor = branches
                .commonAncestor(targetRepo, targetBranch, issuerRepo, issuerBranch)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Branches don't have a common ancestor"));

        Repository repository = repositories.resolve(issuerRepo);
        ObjectId commonAncestorId = commonAncestor.getId();
        ObjectId branchTipId = repository.command(RefParse.class).setName(issuerBranch).call().get()
                .getObjectId();
        log.info("Obtaning PR commits {}...{}", commonAncestor.getId(), commonAncestorId);
        Iterator<RevCommit> commits = repository.command(LogOp.class).setUntil(branchTipId)
                .setSince(commonAncestorId).call();

        return commits;
    }

    /**
     * Performs a diff summary using the PR's test-merge commit as the "old" and the issuer repo as
     * the "new" side of the comparison, for which the PR must be in sync' and the test merge
     * computed
     * 
     * @throws IllegalStateException if there's no merge commit to compute the diff against
     */
    public DiffSummary getDiffSummary(String user, String repo, int prId) {
        final PullRequest pr = this.getOrFail(user, repo, prId);
        return getDiffSummary(pr);
    }

    public DiffSummary getDiffSummary(@NonNull PullRequest pr) {
        final PRStatus status = prWorker.statusInternal(pr);

        final Optional<ObjectId> mergeCommit = status.getMergeCommit();
        Preconditions.checkState(mergeCommit.isPresent(),
                "Merge commit not yet set, can't compute diff summary");

        final PR request = status.getRequest();

        final UUID txId;
        if (Status.MERGED == pr.getStatus() || Status.CLOSED == pr.getStatus()) {
            txId = null;
        } else {// OPEN
            txId = request.getTransactionId();
        }

        final String left = request.getHeadRef();
        final String right = mergeCommit.get().toString();
        final UUID targetRepo = pr.getRepositoryId();

        return repoRpc.diffSummary(targetRepo, targetRepo, left, right, txId, txId);
    }

    /**
     * @see PullRequestWorkerService#merge
     */
    public Task<PullRequestStatus> merge(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer prId,
            @Nullable String commitTitle,
            @Nullable String commitMessage) {//@formatter:on

        final User caller = users.requireAuthenticatedUser();
        final PullRequest pr = getOrFail(user, repo, prId);
        return merge(caller, pr);
    }

    public Task<PullRequestStatus> merge(final User caller, final PullRequest pr) {
        final RepoInfo issuerRepoInfo = repositories.getOrFail(pr.getIssuerRepo());
        UUID issuerRepoOwnerId = issuerRepoInfo.getOwnerId();
        User issuerRepoOwner;
        if (issuerRepoOwnerId.equals(caller.getId())) {
            issuerRepoOwner = caller;
        } else {
            issuerRepoOwner = users.getOrFail(issuerRepoOwnerId);
        }

        String description = String.format("Merge pull request #%d from %s:%s/%s", pr.getId(),
                issuerRepoOwner.getIdentity(), issuerRepoInfo.getIdentity(), pr.getIssuerBranch());

        final ProgressListener listener = new DefaultProgressListener();
        CompletableFuture<PullRequestStatus> future = prWorker.merge(pr, caller.getId(),
                description, listener);

        Supplier<AsyncTaskProgress> progress = () -> {
            AsyncTaskProgress info = new AsyncTaskProgress();
            ProgressListener pl = listener;
            info.setTaskDescription(pl.getDescription());
            info.setProgressDescription(pl.getProgressDescription());
            info.setMaxProgress((double) pl.getMaxProgress());
            info.setProgress((double) pl.getProgress());
            return info;
        };

        ProvidedFutureJob<PullRequestStatus> job = new ProvidedFutureJob<>(future, caller.getId());
        job.setProgressSupplier(progress);
        job.setDescription(description);

        return async.submit(job);
    }

    public PullRequest close(@NonNull UUID caller, @NonNull String user, @NonNull String repo,
            int prId) {

        final PullRequest pr = getOrFail(user, repo, prId);
        return close(caller, pr);
    }

    public PullRequest close(@NonNull UUID caller, @NonNull PullRequest pr) {
        CompletableFuture<PullRequestStatus> close = prWorker.close(pr);
        pr.setClosedByUserId(caller);
        pr.setStatus(Status.CLOSED);
        pr = store.modify(pr);
        try {
            close.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error closing pull request in repository, saved as closed nonetheless", e);
        }
        return pr;
    }

    private void updateStatus(@NonNull PullRequest pr, Status newStatus, @NonNull User issuer) {
        if (pr.getStatus() == newStatus)
            return; // nothing to do

        if (pr.getStatus() == Status.MERGED)
            return; // throw new Exception("cannot un-merge a merged PR!");

        if (newStatus == Status.CLOSED) {
            // close
            pr.setClosedByUserId(issuer.getId());
            pr.setStatus(newStatus);
            pr.setClosedByUserId(users.requireAuthenticatedUser().getId());
        } else {
            // reopen
            pr.setStatus(newStatus);
            pr.setClosedByUserId(null);
        }
    }

    public PullRequest updatePullRequest(String user, String repo, Integer pr,//@formatter:off
            @Nullable String title,
            @Nullable String description,
            @Nullable Boolean open,
            @Nullable String targetBranch) {//@formatter:on

        final User caller = users.requireAuthenticatedUser();
        PullRequest prequest = getOrFail(user, repo, pr);

        if (open != null && prequest.isOpen() != open.booleanValue()) {
            if (prequest.isMerged()) {
                throw new IllegalArgumentException(
                        "Pull request already merged, can't be opened or closed");
            }
            final Status status = prequest.getStatus();
            if (open) {
                checkState(Status.CLOSED.equals(status),
                        "Only CLOSED pull requests can be re-open. Current status: %s", status);
                prWorker.init(prequest).join();// re-init
            } else {
                checkState(Status.CLOSED.equals(status),
                        "Only OPEN pull requests can be closed. Current status: %s", status);
                prWorker.init(prequest).join();// re-init
                prequest = close(caller.getId(), prequest);
            }
            Status newStatus = open.booleanValue() ? Status.OPEN : Status.CLOSED;
            updateStatus(prequest, newStatus, caller);
        }

        if (title != null) {
            prequest.setTitle(title);
        }

        if (description != null) {
            prequest.setDescription(description);
        }

        // TODO - targetBranch --- this is not modifiable!

        prequest = store.modify(prequest);

        return prequest;
    }

    public CompletableFuture<PullRequestStatus> getStatus(@NonNull PullRequest request) {
        return prWorker.status(request);
    }
}
