package org.geogig.web.client.datastore;

import java.io.IOException;
import java.util.UUID;

import org.geogig.web.client.Branch;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geotools.data.Transaction;
import org.geotools.data.Transaction.State;

import com.google.common.base.Preconditions;

class GeogigWebTransactionState implements Transaction.State {

    private Transaction transaction;

    private Repo transactionalRepo;

    GeogigWebTransactionState(Repo transactionalRepo) {
        Preconditions.checkNotNull(transactionalRepo);
        Preconditions.checkArgument(transactionalRepo.getTransaction().isPresent());
        this.transactionalRepo = transactionalRepo;
    }

    private Layer getLayer(String name) {
        Branch currentBranch = transactionalRepo.branches().getCurrentBranch();
        FeatureServiceClient featureService = currentBranch.featureService();
        return featureService.getLayer(name);
    }

    public @Override void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public @Override void addAuthorization(String AuthID) throws IOException {
    }

    public @Override void commit() throws IOException {
        try {
            transactionalRepo.commitSync();
        } catch (RuntimeException e) {
            throw new IOException(e.getCause());
        }
    }

    public @Override void rollback() throws IOException {
        try {
            transactionalRepo.abort();
        } catch (RuntimeException e) {
            throw new IOException(e.getCause());
        }
    }

    public static Layer resolveLayer(Transaction transaction, Layer nonTransactionalLayer) {
        if (null == transaction || Transaction.AUTO_COMMIT == transaction) {
            return nonTransactionalLayer;
        }
        UUID key = nonTransactionalLayer.getRepo().getId();
        State state = transaction.getState(key);
        Preconditions.checkState(state instanceof GeogigWebTransactionState);
        GeogigWebTransactionState gstate = (GeogigWebTransactionState) state;
        Layer txLayer = gstate.getLayer(nonTransactionalLayer.getName());
        return txLayer;
    }

    public static void startTransaction(Transaction transaction, GeogigWebDatastore datastore) {
        Repo nonTransactionalRepo = datastore.getRepo();
        UUID key = nonTransactionalRepo.getId();
        State state = transaction.getState(key);
        if (state == null) {
            Repo transactionalRepo = datastore.startTransaction();
            state = new GeogigWebTransactionState(transactionalRepo);
            transaction.putState(key, state);
        }
    }
}