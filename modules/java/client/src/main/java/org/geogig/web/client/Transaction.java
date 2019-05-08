package org.geogig.web.client;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.TransactionInfo;

public class Transaction extends Identified<TransactionInfo> {

    public Transaction(Client client, TransactionInfo txInfo) {
        super(client, txInfo, () -> txInfo.getId());
    }

    public AsyncTask<TransactionInfo> commit(@Nullable String messageTitle,
            @Nullable String messageAbstract) {
        AsyncTask<TransactionInfo> commitTask = client.transactions().commit(this.getInfo(),
                messageTitle, messageAbstract);
        return commitTask;
    }

    public TransactionInfo abort() {
        TransactionInfo aborted = client.transactions().abort(this.getInfo());
        super.updateInfo(aborted);
        return aborted;
    }
}
