package org.geogig.server.service.rpc;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTaskProgress;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.transaction.TransactionService;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;
import org.locationtech.geogig.plumbing.remotes.RemoteAddOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CheckoutResult;
import org.locationtech.geogig.porcelain.MergeConflictsException;
import org.locationtech.geogig.porcelain.MergeOp.CommitAncestorPair;
import org.locationtech.geogig.porcelain.MergeOp.MergeReport;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PullResult;
import org.locationtech.geogig.remotes.RemoteListOp;
import org.locationtech.geogig.remotes.SynchronizationException;
import org.locationtech.geogig.remotes.SynchronizationException.StatusCode;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class PullBranchJob implements Job<MergeResult> {

    private final @NonNull Transaction transaction;

    private final @NonNull PullArgs args;

    private final @NonNull User issuer;

    private transient GeogigTransaction context;

    private final ProgressListener progress = new DefaultProgressListener();

    private RepositoryManagementService repos;

    public @Override UUID getCallerUser() {
        return issuer.getId();
    }

    public @Override String getDescription() {
        return String.format("Synchronize branch %s", args.getTargetBranch());
    }

    public @Override Optional<UUID> getTransaction() {
        return Optional.of(transaction.getId());
    }

    public @Override AsyncTaskProgress getProgressListener() {
        AsyncTaskProgress info = new AsyncTaskProgress();
        ProgressListener pl = this.progress;
        if (pl != null) {
            info.setTaskDescription(pl.getDescription());
            info.setProgressDescription(pl.getProgressDescription());
            info.setMaxProgress((double) pl.getMaxProgress());
            info.setProgress((double) pl.getProgress());
        }
        return info;
    }

    public @Override CompletableFuture<MergeResult> run(ApplicationContext context) {
        TransactionService service = context.getBean(TransactionService.class);
        repos = context.getBean(RepositoryManagementService.class);
        try {
            this.context = service.resolve(transaction);
            return CompletableFuture.completedFuture(run());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private Remote resolveRemote() {

        ImmutableList<Remote> remotes = context.command(RemoteListOp.class).call();

        final RepoInfo remoteRepoInfo = repos.getOrFail(args.getRemoteRepo());
        final URI sourceRepoURI = repos.resolveRepositoryURI(remoteRepoInfo.getId());
        final String fetchURI = sourceRepoURI.toString();

        Optional<Remote> match = remotes.stream().filter(r -> fetchURI.equals(r.getFetchURL()))
                .findFirst();

        if (match.isPresent()) {
            return match.get();
        }

        String remoteName = repos.compositeName(remoteRepoInfo).replaceAll(":", "_");
        Remote remote = context.command(RemoteAddOp.class).setName(remoteName).setURL(fetchURI)
                .call();
        return remote;
    }

    private MergeResult run() {
        checkout(args.getTargetBranch());

        final Remote remote = resolveRemote();

        PullOp pull = context.command(PullOp.class);
        pull.setIncludeIndexes(true);
        pull.setProgressListener(progress);
        pull.setMessage(args.getCommitMessage());
        pull.setRemote(remote);
        pull.setNoFastForward(args.isNoFf());
        pull.addRefSpec(args.getRemoteBranch());

        PullResult result;
        try {
            result = pull.call();
        } catch (MergeConflictsException mce) {
            return toFailedMergeResult(mce);
        }

        return toMergeResult(result);

    }

    private MergeResult toMergeResult(PullResult cmdPullResult) {
        MergeReport mergeReport = cmdPullResult.getMergeReport().orNull();
        MergeResult mergeResult;
        if (mergeReport == null) {
            throw new SynchronizationException(StatusCode.NOTHING_TO_PUSH);
        } else {
            mergeResult = toMergeResult(mergeReport);
        }
        return mergeResult;
    }

    private Ref checkout(String targetBranch) {
        CheckoutResult res = context.command(CheckoutOp.class).setSource(targetBranch).call();
        Preconditions
                .checkState(CheckoutResult.Results.CHECKOUT_LOCAL_BRANCH.equals(res.getResult()));
        return res.getNewRef();
    }

    private MergeResult toFailedMergeResult(MergeConflictsException ce) {
        MergeResult.MergeResultBuilder builder = MergeResult.builder().success(false);
        MergeScenarioReport report = ce.getReport();
        builder.report(report);
        return builder.build();
    }

    private MergeResult toMergeResult(
            @NonNull org.locationtech.geogig.porcelain.MergeOp.MergeReport report) {

        final @Nullable RevCommit mergeResultCommit = report.getMergeCommit();
        final @Nullable RevCommit mergedCommit;
        final @Nullable RevCommit baseCommit;
        final @Nullable RevCommit ancestorCommit;

        ObjectId oursId = report.getOurs();
        List<CommitAncestorPair> pairs = report.getPairs();
        CommitAncestorPair commitAncestorPair = pairs.get(0);
        ObjectId ancestorId = commitAncestorPair.getAncestor();
        ObjectId theirsId = commitAncestorPair.getTheirs();

        ObjectDatabase db = context.objectDatabase();
        ancestorCommit = ancestorId == null ? null : db.getCommit(ancestorId);
        baseCommit = oursId == null ? null : db.getCommit(oursId);
        mergedCommit = theirsId == null ? null : db.getCommit(theirsId);

        MergeResult.MergeResultBuilder mergeResult = MergeResult.builder();
        mergeResult.success(true);

        mergeResult.mergeCommit(mergeResultCommit);
        mergeResult.oursCommit(baseCommit);
        mergeResult.theirsCommit(mergedCommit);
        mergeResult.commonAncestor(ancestorCommit);
        mergeResult.report(report.getReport().orNull());

        return mergeResult.build();
    }
}
