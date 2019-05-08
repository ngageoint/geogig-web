package org.geogig.server.app.gateway;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geogig.server.model.Transaction;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.server.api.TransactionManagementApi;
import org.geogig.web.server.api.TransactionManagementApiDelegate;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * API delegate implementation for the swagger-codegen auto-generated
 * {@link TransactionManagementApi}, handles the REST request/response/error handling aspects of the
 * API, and delegates business logic to a {@link TransactionService}.
 */
public @Service class TransactionsServiceApi extends AbstractService
        implements TransactionManagementApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired TransactionService service;

    public @Override ResponseEntity<TransactionInfo> startTransaction(String user, String repo) {

        Transaction transaction;
        try {
            transaction = service.beginTransaction(user, repo);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        return super.ok(presentation.toInfo(transaction));
    }

    public @Override ResponseEntity<AsyncTaskInfo> commitTransaction(String user, String repo,
            UUID transactionId, String messageTitle, String messageAbstract) {
        StringBuilder message = new StringBuilder();
        if (!Strings.isNullOrEmpty(messageTitle)) {
            if (-1 != messageTitle.indexOf('\n')) {
                return super.badRequest(
                        "Message title must consist of a single line of text, found newline character at index %d",
                        messageTitle.indexOf('\n'));
            }
            message.append(messageTitle);
        }
        if (!Strings.isNullOrEmpty(messageAbstract)) {
            if (message.length() > 0) {
                message.append('\n').append('\n');
            }
            message.append(messageAbstract);
        }

        String commitMessage = message.length() == 0 ? null : message.toString();

        return run(HttpStatus.OK, () -> {
            return presentation
                    .toInfo(service.commitTransaction(user, repo, transactionId, commitMessage));
        });
    }

    public @Override ResponseEntity<TransactionInfo> abortTransaction(String user, String repo,
            UUID transactionId) {
        return run(HttpStatus.OK,
                () -> presentation.toInfo(service.abortTransaction(user, repo, transactionId)));
    }

    public @Override ResponseEntity<Void> deleteTransactionInfo(String user, String repo,
            UUID transactionId) {
        return run(HttpStatus.NO_CONTENT, () -> {
            // just to make sure the tx belongs to the specified repo
            service.getTransaction(user, repo, transactionId);
            service.deleteTransaction(transactionId);
            return null;
        });
    }

    public @Override ResponseEntity<TransactionInfo> getTransactionInfo(String user, String repo,
            UUID transactionId) {

        return run(HttpStatus.OK,
                () -> presentation.toInfo(service.getTransaction(user, repo, transactionId)));
    }

    public @Override ResponseEntity<List<TransactionInfo>> listAllTransactions() {
        Iterable<Transaction> transactions = service.getTransactions();
        List<TransactionInfo> list = Lists
                .newArrayList(Iterables.transform(transactions, presentation::toInfo));
        return super.ok(list);
    }

    public @Override ResponseEntity<List<TransactionInfo>> listUserTransactions(String user) {
        Iterable<Transaction> transactions = service.getTransactions(user);
        List<TransactionInfo> list = Lists
                .newArrayList(Iterables.transform(transactions, presentation::toInfo));
        return super.ok(list);
    }

    public @Override ResponseEntity<List<TransactionInfo>> listRepositoryTransactions(String user,
            String repo) {
        Iterable<Transaction> transactions = service.getTransactions(user, repo);
        List<TransactionInfo> list = Lists
                .newArrayList(Iterables.transform(transactions, presentation::toInfo));
        return super.ok(list);
    }
}
