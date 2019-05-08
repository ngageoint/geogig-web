package org.geogig.server.service.transaction;

import static com.google.common.base.Preconditions.checkArgument;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.AsyncTask;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.user.UserService;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.TransactionBegin;
import org.locationtech.geogig.plumbing.TransactionEnd;
import org.locationtech.geogig.plumbing.TransactionResolve;
import org.locationtech.geogig.porcelain.AddOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("TransactionService")
public class TransactionService {

    private @Getter @Autowired RepositoryManagementService repos;

    private @Getter @Autowired UserService users;

    private @Autowired TransactionInfoStore store;

    private @Autowired AsyncTasksService async;

    /**
     * @throws RepositoryConnectionException
     * @throws NoSuchElementException
     * @throws IllegalArgumentException
     */
    public GeogigTransaction resolve(@NonNull String user, @NonNull String repo, @NonNull UUID txId)
            throws NoSuchElementException, RepositoryConnectionException {

        Transaction tx = getOrFail(txId);
        UUID repositoryId = tx.getRepositoryId();
        RepoInfo repoInfo = repos.getOrFail(repositoryId);
        Preconditions.checkArgument(repoInfo.getIdentity().equals(repo),
                "Transaction does not belong to the specified repository");
        User owner = users.getOrFail(repoInfo.getOwnerId());
        Preconditions.checkArgument(owner.getIdentity().equals(user),
                "Transaction does not belong to the specified repository");

        Context context = resolve(tx);
        return (GeogigTransaction) context;
    }

    public GeogigTransaction resolve(@NonNull UUID txId)
            throws NoSuchElementException, RepositoryConnectionException {

        Transaction tx = getOrFail(txId);
        return resolve(tx);
    }

    public GeogigTransaction resolve(@NonNull Transaction txInfo)
            throws NoSuchElementException, RepositoryConnectionException {
        UUID txId = txInfo.getId();
        UUID repositoryId = txInfo.getRepositoryId();
        Context context = resolveContext(repositoryId, txId);
        return (GeogigTransaction) context;
    }

    public Context resolveContext(String user, String repo, @Nullable UUID txId)
            throws NoSuchElementException, RepositoryConnectionException {

        Repository repository = repos.resolve(user, repo);
        Context context = repository.context();
        if (txId != null) {
            Optional<GeogigTransaction> tx = get(repository, txId);
            checkArgument(tx.isPresent(), "Transaction %s not found on %s/%s", txId, user, repo);
            context = tx.get();
        }
        return context;
    }

    public Context resolveContext(@NonNull UUID repoId, @Nullable UUID txId)
            throws NoSuchElementException, RepositoryConnectionException {

        Repository repository = repos.resolve(repoId);
        Context context = repository.context();
        if (txId != null) {
            Optional<GeogigTransaction> tx = get(repository, txId);
            checkArgument(tx.isPresent(), "Transaction %s not found on repo %s", txId, repoId);
            context = tx.get();
        }
        return context;
    }

    public Context resolveContext(@NonNull RepoInfo repo, @Nullable UUID txId)
            throws NoSuchElementException, RepositoryConnectionException {

        Repository repository = repos.resolve(repo);
        Context context = repository.context();
        if (txId != null) {
            Optional<GeogigTransaction> tx = get(repository, txId);
            checkArgument(tx.isPresent(), "Transaction %s not found on repo %s", txId,
                    repo.getId());
            context = tx.get();
        }
        return context;
    }

    public Optional<GeogigTransaction> get(Repository repo, UUID txId) {
        com.google.common.base.Optional<GeogigTransaction> tx;
        tx = repo.command(TransactionResolve.class).setId(txId).call();

        return Optional.ofNullable(tx.orNull());
    }

    public Transaction beginTransaction(String user, String repo)
            throws NoSuchElementException, RepositoryConnectionException {

        final User creator = users.requireAuthenticatedUser();

        RepoInfo repoInfo = repos.getOrFail(user, repo);
        Repository repository = repos.resolve(user, repo);
        GeogigTransaction tx = repository.command(TransactionBegin.class).call();
        try {
            Transaction tinfo = createTransaction(repoInfo.getOwnerId(), repoInfo.getId(),
                    creator.getId(), tx.getTransactionId());
            return tinfo;
        } catch (Exception e) {
            repository.command(TransactionEnd.class).setCancel(true).setTransaction(tx).call();
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public Transaction beginTransaction(@NonNull UUID repoId)
            throws NoSuchElementException, RepositoryConnectionException {

        final User creator = users.requireAuthenticatedUser();
        return beginTransaction(repoId, creator);
    }

    public Transaction beginTransaction(@NonNull UUID repoId, @NonNull User creator)
            throws NoSuchElementException, RepositoryConnectionException {

        RepoInfo repoInfo = repos.getOrFail(repoId);
        Repository repository = repos.resolve(repoId);
        GeogigTransaction tx = repository.command(TransactionBegin.class).call();
        try {
            Transaction tinfo = createTransaction(repoInfo.getOwnerId(), repoInfo.getId(),
                    creator.getId(), tx.getTransactionId());
            return tinfo;
        } catch (Exception e) {
            repository.command(TransactionEnd.class).setCancel(true).setTransaction(tx).call();
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public Transaction createTransaction(UUID userId, UUID repositoryId, UUID creatorId,
            UUID transactionId) {

        Transaction tinfo = new Transaction();
        tinfo.setId(transactionId);
        tinfo.setStatus(Transaction.Status.OPEN);
        tinfo.setUserId(userId);
        tinfo.setRepositoryId(repositoryId);
        tinfo.setCreatedByUserId(creatorId);
        tinfo.setCreatedAt(OffsetDateTime.now());
        store.create(tinfo);
        return tinfo;
    }

    //@formatter:off
    public AsyncTask commitTransaction(
            String user, 
            String repo, 
            UUID transactionId,
            @Nullable String commitMessage) {
      //@formatter:on

        User invoker = users.requireAuthenticatedUser();

        Transaction transactionInfo = store.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException());

        CommitJob commitJob = new CommitJob(invoker, transactionInfo, commitMessage);

        Task<Transaction> task = async.submit(commitJob);
        return task.getTaskInfo();
    }

    @Async("repoCommandsExecutor")
    public CompletableFuture<Transaction> commit(@NonNull User caller,
            @NonNull Transaction transactionInfo, @Nullable String commitMessage) {

        CompletableFuture<Transaction> future = new CompletableFuture<>();
        try {
            GeogigTransaction tx = resolve(transactionInfo);
            transactionInfo.setStatus(Transaction.Status.COMMITTING);
            transactionInfo.setUpdatedAt(OffsetDateTime.now());

            boolean mergeUnfinished = tx.command(RefParse.class).setName(Ref.MERGE_HEAD).call()
                    .isPresent();
            boolean conflicts = false;
            if (mergeUnfinished) {
                conflicts = tx.conflictsDatabase().hasConflicts(null);
            }
            if ((mergeUnfinished && !conflicts)
                    || (!tx.workingTree().isClean() || !tx.stagingArea().isClean())) {
                tx.command(AddOp.class).call();

                String authorName = caller.getFullName();
                String authorEmail = caller.getEmailAddress();
                if (authorName == null) {
                    authorName = caller.getIdentity();
                }
                RevCommit commit = tx.command(CommitOp.class).setAuthor(authorName, authorEmail)
                        .setAllowEmpty(true).setCommitter(authorName, authorEmail)
                        .setMessage(commitMessage).call();
                log.info("Created commit " + commit);
            }
            tx.commit();
            log.info("Transaction {} committed", transactionInfo.getId());
            transactionInfo.setStatus(Transaction.Status.COMMITTED);
            transactionInfo.setTerminatedAt(OffsetDateTime.now());
            transactionInfo.setTerminatedByUserId(caller.getId());
            store.modify(transactionInfo);
            future.complete(transactionInfo);
        } catch (Exception e) {
            transactionInfo.setStatus(Transaction.Status.OPEN);// the transaction stays open
            future.completeExceptionally(e);
        }
        return future;
    }

    private @AllArgsConstructor static class CommitJob implements Job<Transaction> {

        private @NonNull User caller;

        private @NonNull Transaction transaction;

        private @Nullable String commitMessage;

        public @Override UUID getCallerUser() {
            return caller.getId();
        }

        public @Override CompletableFuture<Transaction> run(ApplicationContext context) {
            TransactionService service = context.getBean(TransactionService.class);
            return service.commit(caller, transaction, commitMessage);
        }

        public @Override String getDescription() {
            return String.format("Commit transaction %s", transaction.getId());
        }

        public @Override Optional<UUID> getTransaction() {
            return Optional.of(transaction.getId());
        }

    }

    public Transaction abortTransaction(String user, String repo, UUID transactionId) {

        final User issuer = users.requireAuthenticatedUser();

        log.info("Aborting transaction {}/{}/{}, requested by {}", user, repo, transactionId,
                issuer.getIdentity());

        return abortTransaction(transactionId, issuer);
    }

    public Transaction abortTransaction(UUID transactionId, final User issuer) {
        Transaction transactionInfo = store.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction info not found for " + transactionId));
        return abortTransaction(transactionInfo, issuer);
    }

    public Transaction abortTransaction(Transaction transactionInfo, final User issuer) {
        try {
            transactionInfo.setStatus(Transaction.Status.ABORTED);
            transactionInfo.setTerminatedAt(OffsetDateTime.now());
            transactionInfo.setTerminatedByUserId(issuer.getId());

            GeogigTransaction tx = resolve(transactionInfo);
            // TODO: make asynchronous
            tx.abort();
            log.info("Transaction aborted: {}, requested by {}", transactionInfo.getId(),
                    issuer.getIdentity());

            return transactionInfo;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        } finally {
            store.deleteById(transactionInfo.getId());
        }
    }

    // TODO: delete transaction on repository (use AOP to keep config/rpc services decoupled?)
    public Transaction deleteTransaction(@NonNull UUID transactionId) {
        return store.deleteById(transactionId);
    }

    public Transaction getOrFail(@NonNull UUID transactionId) {
        return store.findById(transactionId).orElseThrow(
                () -> new NoSuchElementException("Transaction " + transactionId + " not found"));
    }

    public Optional<Transaction> getTransaction(@NonNull UUID transactionId) {
        return store.findById(transactionId);
    }

    public Transaction getTransaction(String user, String repo, UUID transactionId) {
        Optional<Transaction> tx = store.findById(transactionId);
        if (!tx.isPresent()) {
            throw new NoSuchElementException("Transaction not found");
        }
        final RepoInfo repoInfo = repos.getByName(user, repo)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Repo %s:%s not found", user, repo)));

        Transaction transactionInfo = tx.get();

        UUID repository = transactionInfo.getRepositoryId();
        checkArgument(Objects.equals(repoInfo.getId(), repository),
                "The requested transaction doesn't belong to the requested repository");

        return transactionInfo;
    }

    public Iterable<Transaction> getTransactions() {
        return store.findAll();
    }

    public Iterable<Transaction> getTransactions(@NonNull String user) {

        User userInfo = users.getByNameOrFail(user);
        Iterable<Transaction> userTransactions = store.findByUserId(userInfo.getId());
        return userTransactions;
    }

    public Iterable<Transaction> getTransactions(@NonNull String user, @NonNull String repo) {
        RepoInfo repository = repos.getOrFail(user, repo);
        UUID repoId = repository.getId();
        Iterable<Transaction> repoTransactions = store.findByRepositoryId(repoId);
        return repoTransactions;
    }
}
