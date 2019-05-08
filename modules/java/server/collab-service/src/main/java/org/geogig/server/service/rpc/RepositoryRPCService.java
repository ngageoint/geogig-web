package org.geogig.server.service.rpc;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.ConflictFeatureIterator;
import org.geogig.server.service.branch.ConflictTuple;
import org.geogig.server.service.branch.DiffSummary;
import org.geogig.server.service.branch.DiffSummaryService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.user.UserService;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.plumbing.RevParse;
import org.locationtech.geogig.porcelain.AddOp;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

import lombok.NonNull;

@Service("RepositoryRPCService")
public class RepositoryRPCService {

    private @Autowired TransactionService tx;

    private @Autowired UserService users;

    private @Autowired AsyncTasksService async;

    private @Autowired DiffSummaryService diffSummaryService;

    private <T> T run(@NonNull String user, @NonNull String repo, @Nullable UUID txId,
            @NonNull Function<Context, T> function) {
        Context context;
        try {
            context = tx.resolveContext(user, repo, txId);
        } catch (RepositoryConnectionException e) {
            throw new IllegalStateException(e);
        }
        return function.apply(context);
    }

    public <T> T run(@NonNull UUID repoId, @NonNull AbstractGeoGigOp<T> command) {
        return run(repoId, null, context -> {
            command.setContext(context);
            return command.call();
        });
    }

    public <T> T run(@NonNull UUID repoId, @NonNull Function<Context, T> function) {
        return run(repoId, null, function);
    }

    public <T> T run(@NonNull UUID repoId, @Nullable UUID txId,
            @NonNull Function<Context, T> function) {
        Context context;
        try {
            context = tx.resolveContext(repoId, txId);
        } catch (RepositoryConnectionException e) {
            throw new IllegalStateException(e);
        }
        return function.apply(context);
    }

    private <T> T run(@NonNull RepoInfo repo, @Nullable UUID txId,
            @NonNull Function<Context, T> function) {
        Context context;
        try {
            context = tx.resolveContext(repo, txId);
        } catch (RepositoryConnectionException e) {
            throw new IllegalStateException(e);
        }
        return function.apply(context);
    }

    public DiffSummary diffSummary(@NonNull UUID repo, @NonNull String leftTreeish,
            @NonNull String rightTreeish, UUID tx) {
        return diffSummary(repo, repo, leftTreeish, rightTreeish, tx, tx);
    }

    public DiffSummary diffSummary(@NonNull UUID leftRepo, @NonNull UUID rightRepo,
            @NonNull String leftTreeish, @NonNull String rightTreeish, UUID leftRepoTx,
            UUID rightRepoTx) {

        CompletableFuture<DiffSummary> future = diffSummaryService.diffSummary(leftRepo, rightRepo,
                leftTreeish, rightTreeish, leftRepoTx, rightRepoTx);
        return future.join();
    }

    //@formatter:off
    public Iterator<RevCommit> getCommits(
            @NonNull UUID repoId,
            @Nullable String head) {
        //@formatter:on
        return getCommits(repoId, head, null, null, null);
    }

    //@formatter:off
    public Iterator<RevCommit> getCommits(
            @NonNull UUID repoId,
            @Nullable String head,
            @Nullable List<String> path,
            @Nullable Integer limit,
            @Nullable UUID txId) {
        //@formatter:on

        return run(repoId, txId, context -> getCommits(context, head, path, limit));
    }

    public Iterator<RevCommit> getCommits(@NonNull Context context, @Nullable String head) {
        return getCommits(context, head, null, null);
    }

    public Iterator<RevCommit> getCommits(@NonNull Context context, @Nullable String head,
            @Nullable List<String> path, @Nullable Integer limit) {
        //@formatter:on

        LogOp command = context.command(LogOp.class);
        if (null != path) {
            path.stream().filter((s) -> !Strings.isNullOrEmpty(s))
                    .forEach((s) -> command.addPath(s));
        }
        if (limit != null) {
            command.setLimit(limit.intValue());
        }
        if (null != head) {
            ObjectId tip = context.command(RevParse.class).setRefSpec(head).call().orNull();
            Preconditions.checkArgument(tip != null, "%s does not resolve to a commit", head);
            command.setUntil(tip);
        }
        Iterator<RevCommit> commits = command.call();
        return commits;
    }

    public Task<MergeResult> merge(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @Nullable UUID txId,
            @NonNull MergeRequest mergeRequest) {//@formatter:on

        final Transaction transaction;
        final boolean autoCommit;
        User issuer = users.requireAuthenticatedUser();
        if (txId == null) {
            try {
                transaction = tx.beginTransaction(user, repo);
                autoCommit = true;
            } catch (RepositoryConnectionException e) {
                throw new IllegalStateException(
                        String.format("Unable to create transaction on %s:%s"));
            }
        } else {
            transaction = tx.getTransaction(user, repo, txId);
            autoCommit = false;
        }

        MergeJob mergeJob;
        mergeJob = MergeJob.builder().transaction(transaction).autoCommit(autoCommit)
                .transactionService(tx).request(mergeRequest).issuer(issuer).build();
        Task<MergeResult> taskInfo = async.submit(mergeJob);
        return taskInfo;
    }

    public Task<MergeResult> pull(//@formatter:off
            @NonNull User caller,
            @NonNull UUID txId,
            @NonNull PullArgs args) {//@formatter:on

        final Transaction transaction = tx.getOrFail(txId);

        PullBranchJob job = PullBranchJob.builder().transaction(transaction).args(args)
                .issuer(caller).build();

        Task<MergeResult> taskInfo = async.submit(job);
        return taskInfo;
    }

    public long countConflicts(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            @Nullable String path) {//@formatter:on

        return run(user, repo, txId, context -> {
            long count = context.conflictsDatabase().getCountByPrefix(null, path);
            return count;
        });
    }

    public Iterator<ConflictTuple> getConflicts(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            @Nullable String path,
            boolean details,
            int page,
            int pageSize) {//@formatter:on

        Preconditions.checkArgument(page > 0);
        Preconditions.checkArgument(pageSize >= 0);

        return run(user, repo, txId, context -> {
            Iterator<Conflict> conflicts = context.conflictsDatabase().getByPrefix(null, path);
            int skip = (page - 1) * pageSize;
            Iterators.advance(conflicts, skip);
            conflicts = Iterators.limit(conflicts, pageSize);
            if (details) {
                return getDetailedConflicts(conflicts, context);
            }
            return Iterators.transform(conflicts, c -> new ConflictTuple(c));
        });
    }

    private Iterator<ConflictTuple> getDetailedConflicts(Iterator<Conflict> conflicts,
            Context context) {
        if (!conflicts.hasNext()) {
            return Collections.emptyIterator();
        }
        return new ConflictFeatureIterator(conflicts, context);
    }

    public ConflictFeatureIterator getConflictsDetails(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            @NonNull String path,
            @Nullable Integer page,
            @Nullable Integer pageSize) {//@formatter:on

        return run(user, repo, txId, context -> {
            Iterator<Conflict> iterator = context.conflictsDatabase().getByPrefix(null, path);
            if (page != null && pageSize != null) {
                int skip = page * pageSize;
                Iterators.advance(iterator, skip);
                iterator = Iterators.limit(iterator, pageSize);
            } else if (pageSize != null) {
                iterator = Iterators.limit(iterator, pageSize);
            }
            return new ConflictFeatureIterator(iterator, context);
        });
    }

    public RevObject getObject(@NonNull RepoInfo repo, @NonNull ObjectId objectId) {
        return run(repo, null, context -> context.objectDatabase().get(objectId));
    }

    public RevObject getObject(@NonNull UUID repoId, @NonNull ObjectId objectId) {
        return run(repoId, null, context -> context.objectDatabase().get(objectId));
    }

    public RevCommit getCommit(@NonNull UUID repoId, @NonNull ObjectId commitId) {
        return run(repoId, null, context -> context.objectDatabase().getCommit(commitId));
    }

    public Task<Void> stage(@NonNull String user, @NonNull String repo, @NonNull UUID txId,
            List<String> paths) {

        final Transaction transaction = tx.getTransaction(user, repo, txId);
        GeogigTransaction context;
        try {
            context = tx.resolve(transaction);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        User issuer = users.requireAuthenticatedUser();
        AddOp addOp = context.command(AddOp.class);
        if (null != paths) {
            paths.forEach(path -> addOp.addPattern(path));
        }
        Job<Void> job = new SimpleCommandJob<>(addOp, transaction, issuer, w -> null);
        Task<Void> taskInfo = async.submit(job);
        return taskInfo;
    }
}
