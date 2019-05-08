package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.TransactionManagementApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.UserInfo;

import lombok.NonNull;

public class TransactionsClient extends AbstractServiceClient<TransactionManagementApi> {

    TransactionsClient(Client client) {
        super(client, client.transactions);
    }

    private Transaction transaction(TransactionInfo txInfo) {
        return new Transaction(client, txInfo);
    }

    public Transaction startTransaction(RepositoryInfo repository) {
        try {
            String owner = repository.getOwner().getIdentity();
            String repo = repository.getIdentity();
            TransactionInfo txInfo = api.startTransaction(owner, repo);
            checkNotNull(txInfo);
            checkNotNull(txInfo.getRepository());
            checkState(txInfo.getRepository().getId().equals(repository.getId()));
            client.currentTransaction(txInfo.getRepository().getId(), txInfo.getId());
            return transaction(txInfo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public TransactionInfo abort(TransactionInfo transactionInfo) {
        RepositoryInfo repository = transactionInfo.getRepository();
        IdentifiedObject owner = repository.getOwner();
        String user = owner.getIdentity();
        String repo = repository.getIdentity();
        UUID transactionId = transactionInfo.getId();
        try {
            TransactionInfo result = api.abortTransaction(user, repo, transactionId);
            return result;
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public AsyncTask<TransactionInfo> commit(TransactionInfo info) {
        return commit(info, null, null);
    }

    public AsyncTask<TransactionInfo> commit(@NonNull TransactionInfo info,
            @Nullable String commitMessageTitle, @Nullable String commitMessageAbstract) {

        String user = info.getRepository().getOwner().getIdentity();
        String repo = info.getRepository().getIdentity();
        UUID transactionId = info.getId();
        try {
            AsyncTaskInfo taskInfo = api.commitTransaction(user, repo, transactionId,
                    commitMessageTitle, commitMessageAbstract);
            return new AsyncTask<TransactionInfo>(client, taskInfo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public List<Transaction> getAllTransactions() {
        List<TransactionInfo> txInfos;
        try {
            txInfos = api.listAllTransactions();
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return txInfos.stream().map(this::transaction).collect(Collectors.toList());
    }

    public List<Transaction> getTransactions(@NonNull UserInfo user) {
        List<TransactionInfo> txInfos;
        try {
            txInfos = api.listUserTransactions(user.getIdentity());
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return txInfos.stream().map(this::transaction).collect(Collectors.toList());
    }

    public List<Transaction> getTransactions(@NonNull RepositoryInfo repository) {
        String user = repository.getOwner().getIdentity();
        String repo = repository.getIdentity();
        List<TransactionInfo> txInfos;
        try {
            txInfos = api.listRepositoryTransactions(user, repo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return txInfos.stream().map(this::transaction).collect(Collectors.toList());
    }

    public Transaction getById(@NonNull String user, @NonNull String repo,
            @NonNull UUID transactionId) {
        try {
            TransactionInfo txInfo = api.getTransactionInfo(user, repo, transactionId);
            return transaction(txInfo);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }
}
