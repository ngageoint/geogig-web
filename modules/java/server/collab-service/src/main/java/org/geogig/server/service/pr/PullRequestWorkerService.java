package org.geogig.server.service.pr;

import static org.geogig.server.model.PullRequestStatus.MergeableStatus.CHECKING;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.MERGEABLE;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.MERGED;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.MERGING;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.UNKNOWN;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.UNMERGEABLE;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.geogig.commands.pr.ChaninedCommand;
import org.geogig.commands.pr.PR;
import org.geogig.commands.pr.PRCloseOp;
import org.geogig.commands.pr.PRCommand;
import org.geogig.commands.pr.PRHealthCheckOp;
import org.geogig.commands.pr.PRInitOp;
import org.geogig.commands.pr.PRMergeOp;
import org.geogig.commands.pr.PRPrepareOp;
import org.geogig.commands.pr.PRStatus;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequest.Status;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.PullRequestStatus.MergeableStatus;
import org.geogig.server.model.PullRequestStatus.PullRequestStatusBuilder;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.service.pr.autoupdate.AutoUpdatePrCommandHook;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.transaction.TransactionService;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;
import org.locationtech.geogig.porcelain.MergeConflictsException;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.ProgressListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import lombok.NonNull;
import lombok.Value;

@EnableAsync
@Service
public class PullRequestWorkerService {

    private @Autowired RepositoryManagementService repositories;

    private @Autowired TransactionService transactions;

    private @Autowired RepositoryRPCService repoRpc;

    private @Autowired PullRequestStore store;

    private @Autowired ApplicationEventPublisher appbus;

    //@formatter:off
    private static @Value class Key {
        private @NonNull UUID repoId;
        private @NonNull Integer prId;
    }
    public static @Value class InitEvent {private PR pr;private Throwable error;}
    public static @Value class PrepareStartEvent { private PullRequest request;}
    public static @Value class PRStatusSchanged {UUID issuer; PullRequestStatus status;}
    //@formatter:on

    private Key key(PullRequest req) {
        return new Key(req.getRepositoryId(), req.getId());
    }

    private ConcurrentMap<Key, PRCommand<?>> runningCommands = new ConcurrentHashMap<>();

    public @Async CompletableFuture<PR> init(@NonNull PullRequest request) {
        final PR repoPR;
        try {
            final URI remoteURI = repositories.resolveRepositoryURI(request.getIssuerRepo());

            repoPR = repoRpc.run(request.getRepositoryId(), c -> {
                PRInitOp init = PRInitOp.builder()//
                        .id(request.getId())//
                        .remoteURI(remoteURI)//
                        .remoteBranch(request.getIssuerBranch())//
                        .targetBranch(request.getTargetBranch())//
                        .title(request.getTitle())//
                        .description(request.getDescription())//
                        .build();
                init.setProgressListener(new DefaultProgressListener());
                init.setContext(c);
                return init.call();
            });
        } catch (Exception e) {
            appbus.publishEvent(new InitEvent(null, e));
            return CompletableFuture.failedFuture(e);
        }
        appbus.publishEvent(new InitEvent(repoPR, null));
        CompletableFuture<PR> future = CompletableFuture.completedFuture(repoPR);
        return future;
    }

    public @Async CompletableFuture<PullRequestStatus> status(@NonNull PullRequest request) {
        PRCommand<?> running = runningCommands.get(key(request));
        if (running != null) {
            return adaptLongRunningCommandToStatus(request, running);
        }

        final PRStatus status = repoRpc.run(request.getRepositoryId(), c -> {
            return c.command(PRHealthCheckOp.class).setId(request.getId()).call();
        });

        PullRequestStatus ret = toPrStatus(request, status);
        return CompletableFuture.completedFuture(ret);
    }

    PRStatus statusInternal(@NonNull PullRequest request) {
        final PRStatus status = repoRpc.run(request.getRepositoryId(), c -> {
            return c.command(PRHealthCheckOp.class).setId(request.getId()).call();
        });
        return status;
    }

    private PullRequestStatus toPrStatus(@NonNull PullRequest pr, @NonNull PRStatus status) {
        final MergeableStatus mergeable;
        if (status.isConflicted()) {
            mergeable = UNMERGEABLE;
        } else if (status.isMerged()) {
            mergeable = MERGED;
        } else if (status.isRemoteBranchBehind() || status.isTargetBranchBehind()) {
            mergeable = UNKNOWN;
        } else if (status.getMergeCommit().isPresent()) {
            mergeable = MERGEABLE;
        } else {
            mergeable = UNKNOWN;
        }
        return toPrStatus(pr, status, mergeable);
    }

    private PullRequestStatus toPrStatus(@NonNull PullRequest pr, @NonNull PRStatus status,
            MergeableStatus mergeable) {

        UUID transactionId = status.getRequest().getTransactionId();
        final String headRef = status.getRequest().getHeadRef();
        final String originRef = status.getRequest().getOriginRef();
        final String mergeRef = status.getRequest().getMergeRef();

        PullRequestStatusBuilder builder = PullRequestStatus.builder();
        builder.transaction(transactionId);
        builder.request(pr);
        builder.closed(status.isClosed());
        builder.merged(status.isMerged());
        builder.mergeable(mergeable);
        builder.commitsBehindRemoteBranch(status.getCommitsBehindRemoteBranch());
        builder.commitsBehindTargetBranch(status.getCommitsBehindTargetBranch());
        builder.numConflicts(status.getNumConflicts());
        builder.mergeCommitId(status.getMergeCommit().map(id -> id.toString()));
        // builder.report(status.getReport());
        builder.affectedLayers(status.getAffectedLayers());
        builder.headRef(headRef);
        builder.originRef(originRef);
        builder.mergeRef(mergeRef);
        return builder.build();
    }

    public @Async CompletableFuture<PullRequestStatus> prepareAbortIfRunning(
            @NonNull PullRequest pr) {
        return prepare(pr, pr.getIssuerUser(), true);
    }

    public @Async CompletableFuture<PullRequestStatus> prepare(@NonNull PullRequest pr,
            @NonNull UUID issuerId) {
        return prepare(pr, issuerId, false);
    }

    private CompletableFuture<PullRequestStatus> prepare(@NonNull PullRequest pr,
            @NonNull UUID issuerId, boolean abortRunning) {
        final Key key = key(pr);

        final PRPrepareOp cmd = new PRPrepareOp().setId(pr.getId());
        PRCommand<?> runningCommand = runningCommands.putIfAbsent(key, cmd);

        if (runningCommand != null) {
            if (abortRunning && runningCommand instanceof PRPrepareOp) {
                runningCommand.abort();
                runningCommand = runningCommands.put(key, cmd);
            } else {
                return adaptLongRunningCommandToStatus(pr, runningCommand);
            }
        }
        appbus.publishEvent(new PrepareStartEvent(pr));
        try {
            cmd.setProgressListener(new DefaultProgressListener());
            final PRStatus status = repoRpc.run(pr.getRepositoryId(), cmd);

            PullRequestStatus ret = toPrStatus(pr, status);
            ret = updateStatus(issuerId, ret);
            return CompletableFuture.completedFuture(ret);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        } finally {
            runningCommands.remove(key, cmd);
        }
    }

    public @Async CompletableFuture<PullRequestStatus> merge(@NonNull PullRequest pr,
            @NonNull UUID issuerId, @Nullable String message, @Nullable ProgressListener listener) {

        final Key key = key(pr);

        PRCommand<PRStatus> mergeOp = new PRMergeOp().setId(pr.getId()).setMessage(message);
        mergeOp.getClientData().put(AutoUpdatePrCommandHook.DISABLE, Boolean.TRUE);

        final PRCommand<?> runningCommand = runningCommands.putIfAbsent(key, mergeOp);
        if (runningCommand != null) {
            if (runningCommand instanceof PRMergeOp || runningCommand instanceof ChaninedCommand) {
                return adaptLongRunningCommandToStatus(pr, runningCommand);
            } else if (runningCommand instanceof PRPrepareOp) {
                mergeOp = ChaninedCommand.chain(runningCommand, mergeOp);
                runningCommands.put(key, mergeOp);
            }
        }

        PRStatus status;
        try {
            mergeOp.setProgressListener(
                    listener == null ? new DefaultProgressListener() : listener);
            status = repoRpc.run(pr.getRepositoryId(), mergeOp);
        } catch (MergeConflictsException mce) {
            status = repoRpc.run(pr.getRepositoryId(), new PRHealthCheckOp().setId(pr.getId()));
            Optional<MergeScenarioReport> report = Optional.ofNullable(mce.getReport());
            status.withReport(report);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            runningCommands.remove(key);
        }
        PullRequestStatus ret = toPrStatus(pr, status);
        ret = updateStatus(issuerId, ret);
        return CompletableFuture.completedFuture(ret);
    }

    private CompletableFuture<PullRequestStatus> adaptLongRunningCommandToStatus(PullRequest pr,
            final PRCommand<?> runningCommand) {

        MergeableStatus mergeable;
        if (runningCommand instanceof PRPrepareOp) {
            mergeable = CHECKING;
        } else if (runningCommand instanceof PRMergeOp
                || runningCommand instanceof ChaninedCommand) {
            mergeable = MERGING;
        } else {
            throw new IllegalStateException(
                    String.format("Unknown mapping from %s to MergeableStatus",
                            runningCommand.getClass().getName()));
        }
        final PRStatus status = repoRpc.run(pr.getRepositoryId(), c -> {
            return c.command(PRHealthCheckOp.class).setId(pr.getId()).call();
        });
        PullRequestStatus prStatus = toPrStatus(pr, status, mergeable);
        return CompletableFuture.completedFuture(prStatus);
    }

    public @Async CompletableFuture<PullRequestStatus> close(@NonNull PullRequest pr) {
        PRCommand<?> running = runningCommands.remove(key(pr));
        if (running != null) {
            running.abort();
        }
        PRStatus status = repoRpc.run(pr.getRepositoryId(), new PRCloseOp().setId(pr.getId()));
        return CompletableFuture.completedFuture(toPrStatus(pr, status));
    }

    public @Async CompletableFuture<List<PullRequest>> findPullRequestsAffectedByBranch(
            @NonNull URI repositoryURI, @NonNull Ref ref) {

        Optional<RepoInfo> repoOptional = repositories.getByURI(repositoryURI);
        List<PullRequest> prsAffected = Collections.emptyList();
        if (repoOptional.isPresent()) {
            RepoInfo repo = repoOptional.get();
            UUID repoId = repo.getId();
            prsAffected = Streams.stream(store.findAll())//@formatter:off
                    .filter(p -> prIsAffectedBy(p, repoId, ref)
            ).collect(Collectors.toList());//@formatter:on
        }

        return CompletableFuture.completedFuture(prsAffected);
    }

    private boolean prIsAffectedBy(final PullRequest p, final UUID branchRepoId, final Ref branch) {
        if (p.isClosed() || p.isMerged()) {
            return false;
        }
        String branchName = branch.getName();
        UUID issuerRepo = p.getIssuerRepo();
        UUID targetRepo = p.getRepositoryId();
        String issuerBranch = p.getIssuerBranch();
        String targetBranch = p.getTargetBranch();
        if (branchRepoId.equals(issuerRepo)) {
            if (branchName.equals(issuerBranch) || branch.localName().equals(issuerBranch)) {
                return true;
            }
        }
        if (branchRepoId.equals(targetRepo)) {
            if (branchName.equals(targetBranch) || branch.localName().equals(targetBranch)) {
                return true;
            }
        }
        return false;
    }

    public PullRequestStatus updateStatus(@NonNull UUID callerUserId,
            @NonNull PullRequestStatus status) {

        PullRequest req = status.getRequest();
        UUID transaction = status.getTransaction();

        if (status.isMerged()) {
            req.setStatus(Status.MERGED);
            req.setClosedByUserId(callerUserId);
            transactions.deleteTransaction(transaction);
        } else if (status.isClosed()) {
            req.setStatus(Status.CLOSED);
            req.setClosedByUserId(callerUserId);
            transactions.deleteTransaction(transaction);
        }
        req = store.modify(req);
        status = status.withRequest(req);
        appbus.publishEvent(new PRStatusSchanged(callerUserId, status));
        return status;
        // TODO: save status?
    }

}
