package org.geogig.server.service.branch;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTaskProgress;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.branch.RevertResult.RevertResultBuilder;
import org.geogig.server.service.transaction.TransactionService;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.ResolveCommit;
import org.locationtech.geogig.porcelain.BranchResolveOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CheckoutResult;
import org.locationtech.geogig.porcelain.RevertConflictsException;
import org.locationtech.geogig.porcelain.RevertOp;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@Data
public class RevertJob implements Job<RevertResult> {

    /**
     * Handle to the transaction where to run the op
     */
    private final @NonNull Transaction transaction;

    /**
     * Branch on which to revert the commit, and whose history the commit to revert is part of
     */
    private final @NonNull String branch;

    /**
     * Commit to revert
     */
    private final @NonNull String commitIsh;

    /**
     * User performing the revert
     */
    private final @NonNull User issuer;

    private final boolean autoCommit;

    private transient GeogigTransaction context;

    private final ProgressListener progress = new DefaultProgressListener();

    public @Override UUID getCallerUser() {
        return issuer.getId();
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

    public @Override String getDescription() {
        UUID repoId = transaction.getRepositoryId();
        return String.format("Revert commit %s of repo %s at branch %s", commitIsh, repoId, branch);
    }

    public @Override Optional<UUID> getTransaction() {
        return Optional.of(transaction.getId());
    }

    public @Override CompletableFuture<RevertResult> run(ApplicationContext context) {
        TransactionService service = context.getBean(TransactionService.class);
        try {
            this.context = service.resolve(transaction);
            return CompletableFuture.completedFuture(run());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private RevertResult run() {
        final Ref branchRef = context.command(BranchResolveOp.class).setName(branch).call()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Branch %s not found", branch)));

        if (branchRef.getName().startsWith(Ref.TAGS_PREFIX)) {
            throw new IllegalArgumentException("A tag's commit cannot be reset");
        }

        final RevCommit revertedCommit = context.command(ResolveCommit.class).setCommitIsh(commitIsh).call()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("ref-spec %s is not found", commitIsh)));

        try {
            CheckoutResult checkoutResult = context.command(CheckoutOp.class)
                    .setSource(branchRef.getName()).call();
            Stopwatch sw = Stopwatch.createStarted();

            RevertResultBuilder result = RevertResult.builder();
            result.repository(transaction.getRepositoryId());
            result.branch(this.branch);
            result.revertCommit(revertedCommit.getId());
            try {
                RevertOp revert = context.command(RevertOp.class).setCreateCommit(true)
                        .addCommit(() -> revertedCommit.getId());
                revert.setProgressListener(progress);

                String authorName = issuer.getFullName() == null ? issuer.getIdentity()
                        : issuer.getFullName();
                String authorEmail = issuer.getEmailAddress();
                revert.getClientData().put("user.name", authorName);
                revert.getClientData().put("user.email", authorEmail);

                revert.call();

                log.debug("Reverted commit {} in {}", revertedCommit.getId(), sw.stop());

                Ref updatedRef = context.command(RefParse.class).setName(branchRef.getName()).call()
                        .get();

                if (autoCommit) {
                    context.commit();
                }

                final RevCommit revertCommit = context.objectDatabase()
                        .getCommit(updatedRef.getObjectId());

                result.success(true);
                result.revertedCommit(revertedCommit.getId());
                result.revertCommit(revertCommit.getId());
            } catch (RevertConflictsException ce) {
                log.info("Revert failed", ce);
                result.success(false);
                if (autoCommit) {
                    context.abort();
                }
            }

            return result.build();
        } catch (Exception e) {
            try {
                context.command(RevertOp.class).setAbort(true).call();
            } catch (RuntimeException ignore) {
                log.info("Ignoring exception while aborting revert", e);
            } finally {
                if (autoCommit) {
                    context.abort();
                }
            }

            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

}
