package org.geogig.server.service.rpc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.transaction.TransactionService;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.RevParse;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;
import org.locationtech.geogig.porcelain.BranchResolveOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CheckoutResult;
import org.locationtech.geogig.porcelain.MergeConflictsException;
import org.locationtech.geogig.porcelain.MergeOp;
import org.locationtech.geogig.porcelain.MergeOp.CommitAncestorPair;
import org.locationtech.geogig.porcelain.NothingToCommitException;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.springframework.context.ApplicationContext;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class MergeJob implements Job<MergeResult> {

    private final @NonNull Transaction transaction;

    private final @NonNull MergeRequest request;

    private final @NonNull User issuer;

    private final @NonNull TransactionService transactionService;

    private final boolean autoCommit;

    private transient GeogigTransaction context;

    public @Override UUID getCallerUser() {
        return issuer.getId();
    }

    public @Override String getDescription() {
        UUID repo = transaction.getRepositoryId();
        RepoInfo repoInfo = transactionService.getRepos().getOrFail(repo);
        User repoOwner = transactionService.getUsers().getOrFail(repoInfo.getOwnerId());
        String head = request.getHead();
        String base = request.getBase();
        String user = repoOwner.getIdentity();
        String repoName = repoInfo.getIdentity();
        return String.format("Merge %s onto %s:%s/%s", head, user, repoName, base);
    }

    public @Override Optional<UUID> getTransaction() {
        return Optional.of(transaction.getId());
    }

    public @Override CompletableFuture<MergeResult> run(ApplicationContext context) {
        TransactionService service = context.getBean(TransactionService.class);
        try {
            this.context = service.resolve(transaction);
            return CompletableFuture.completedFuture(run());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private MergeResult run() {
        final Optional<Ref> currentBranch = context.command(BranchResolveOp.class).call();

        final Ref targetBranch = resolveTargetBranch(request.getBase(), currentBranch);

        checkout(targetBranch);

        MergeOp command = context.command(MergeOp.class);
        String head = request.getHead();
        ObjectId headId = context.command(RevParse.class).setRefSpec(head).call().orNull();
        if (null == headId) {
            throw new IllegalArgumentException(
                    String.format("'%s' does not resolve to a commit", head));
        }
        command.addCommit(headId);
        command.setNoFastForward(request.isNoFf());

        command.setMessage(request.getCommitMessage());

        final String authorName = issuer.getFullName() == null ? issuer.getIdentity()
                : issuer.getFullName();
        final @Nullable String authorEmail = issuer.getEmailAddress();
        command.setAuthor(authorName, authorEmail);

        MergeResult result;
        try {
            org.locationtech.geogig.porcelain.MergeOp.MergeReport cmdResult = command.call();
            result = toMergeResult(cmdResult);
        } catch (NothingToCommitException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (MergeConflictsException ce) {
            result = toFailedMergeResult(ce);
        }

        final boolean success = result.isSuccess();
        if (success) {
            if (currentBranch.isPresent()) {
                checkout(currentBranch.get());
            }
            if (autoCommit) {
                transactionService.commit(issuer, transaction, null);
            }
        }
        return result;
    }

    private Ref checkout(Ref targetBranch) {
        CheckoutResult res = context.command(CheckoutOp.class).setSource(targetBranch.getName())
                .call();
        return res.getNewRef();
    }

    private Ref resolveTargetBranch(String base, Optional<Ref> currentBranch) {
        if (currentBranch.isPresent()) {
            String name = currentBranch.get().getName();
            if (name.equals(base) || NodeRef.nodeFromPath(name).equals(base)) {
                return currentBranch.get();
            }
        }
        Ref branch = context.command(RefParse.class).setName(base).call().orNull();
        if (null == branch) {
            throw new IllegalArgumentException("Branch " + base + " does not exist");
        }

        return branch;
    }

    private MergeResult toFailedMergeResult(MergeConflictsException ce) {
        MergeResult.MergeResultBuilder builder = MergeResult.builder().success(false);
        MergeScenarioReport report = ce.getReport();
        builder.report(report);
        return builder.build();
    }

    private MergeResult toMergeResult(
            org.locationtech.geogig.porcelain.MergeOp.MergeReport report) {

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
