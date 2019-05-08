package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geogig.web.client.AsyncTask;
import org.geogig.web.client.Branch;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.Error;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MergeTest extends AbstractIntegrationTest {

    private Store store;

    private User user;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo("naturalEearth");
    }

    public @Test void mergeNoOp() throws Exception {
        Layer poi = createPoiLayer(repo);
        Branch master = repo.branches().getCurrentBranch();
        Branch branch = poi.getBranch().branch("branch1");
        repo.startTransaction();
        AsyncTask<MergeResult> asyncResult = master.merge(branch.getName());
        asyncResult.awaitTermination();
        assertComplete(asyncResult);
    }

    public @Test void mergeInvalidHead() throws Exception {
        createPoiLayer(repo);
        Branch master = repo.branches().getCurrentBranch();
        repo.startTransaction();

        AsyncTask<MergeResult> asyncResult = master.merge("non-existent-head");
        asyncResult.awaitTermination();

        AsyncTaskInfo taskInfo = asyncResult.getInfo();
        assertTrue(asyncResult.isFinished());
        assertFalse(asyncResult.isComplete());
        assertEquals(StatusEnum.FAILED, asyncResult.getStatus());
        assertNotNull(taskInfo.getTransaction());
        assertEquals(TransactionStatus.OPEN, taskInfo.getTransaction().getStatus());

        Error error = taskInfo.getError();
        assertNotNull(error);
        assertNotNull(error.getMessage());
        repo.abort();
    }

    public @Test void mergeInvalidBase() throws Exception {
        createPoiLayer(repo);
        repo.startTransaction();
        AsyncTask<MergeResult> asyncResult = repo.branches().merge("invalid-base", "master");

        asyncResult.awaitTermination();

        AsyncTaskInfo taskInfo = asyncResult.getInfo();
        assertTrue(asyncResult.isFinished());
        assertFalse(asyncResult.isComplete());
        assertEquals(StatusEnum.FAILED, asyncResult.getStatus());
        assertNotNull(taskInfo.getTransaction());
        assertEquals(TransactionStatus.OPEN, taskInfo.getTransaction().getStatus());

        Error error = taskInfo.getError();
        assertNotNull(error);
        assertNotNull(error.getMessage());
        repo.abort();
    }

    public @Test void mergeNoChangesInBase() throws Exception {
        createPoiLayer(repo);

        Branch master = repo.branches().getCurrentBranch();
        Branch branch = master.branch("branch1");

        repo.startTransaction();
        branch.checkout();

        GeogigFeature[] features = testSupport.poiFeatures();
        GeogigFeatureCollection collection = GeogigFeatureCollection
                .of(testSupport.poiFeatureType(), features);
        branch.featureService().addFeatures(collection);
        repo.commitSync("add features to branch", null);

        branch = branch.refresh();
        assertNotEquals(master.getCommit().getId(), branch.getCommit().getId());

        repo.startTransaction();
        master.checkout();
        AsyncTask<MergeResult> asyncResult = master.merge(branch.getName());
        asyncResult.awaitTermination();

        assertComplete(asyncResult);

        MergeResult result = asyncResult.getResult();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(features.length, result.getMergeReport().getUnconflictedCount().longValue());
        repo.commitSync();
        assertNotEquals(branch.getCommit().getId(), master.getCommit().getId());
        master.refresh();
        assertEquals(branch.getCommit().getId(), master.getCommit().getId());
    }

    public @Test void mergeFastForward() throws Exception {
        createPoiLayer(repo);

        Branch master = repo.branches().getCurrentBranch();
        Branch branch = master.branch("branch1");

        GeogigFeature[] features = testSupport.poiFeatures();
        RevisionFeatureType type = testSupport.poiFeatureType();
        {
            repo.startTransaction();
            master.featureService()
                    .addFeatures(GeogigFeatureCollection.of(type, features[0], features[1]));
            repo.commitSync("add features to master", null);
        }
        {
            repo.startTransaction();
            branch.checkout();
            branch.featureService()
                    .addFeatures(GeogigFeatureCollection.of(type, features[2], features[3]));
            repo.commitSync("add features to branch", null);
        }

        branch = branch.refresh();
        master = master.refresh();

        repo.startTransaction();
        master.checkout();
        AsyncTask<MergeResult> asyncResult = master.merge(branch.getName());
        asyncResult.awaitTermination();

        assertComplete(asyncResult);

        MergeResult result = asyncResult.getResult();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getMergeReport().getUnconflictedCount().longValue());
        repo.commitSync();
        assertNotEquals(branch.getCommit().getId(), master.getCommit().getId());
        master.refresh();
        long size = master.featureService().getSize(type.getName(), new FeatureQuery());
        assertEquals(4, size);
    }

    public @Test void mergeConflict() throws Exception {
        createPoiLayer(repo);

        Branch master = repo.branches().getCurrentBranch();
        Branch branch = master.branch("branch1");

        GeogigFeature[] features = testSupport.poiFeatures();
        RevisionFeatureType type = testSupport.poiFeatureType();
        {
            repo.startTransaction();
            master.featureService()
                    .addFeatures(GeogigFeatureCollection.of(type, features[0], features[1]));
            repo.commitSync("add features to master", null);
        }
        {// both add features[0] (fid-1), with different name attribute values
            repo.startTransaction();
            features[0].put("name", "Changed name");
            branch.checkout();
            branch.featureService().addFeatures(
                    GeogigFeatureCollection.of(type, features[0], features[2], features[3]));
            repo.commitSync("add features to branch", null);
        }

        branch = branch.refresh();
        master = master.refresh();

        repo.startTransaction();
        master.checkout();
        AsyncTask<MergeResult> asyncResult = master.merge(branch.getName());
        asyncResult.awaitTermination();

        assertTrue(asyncResult.isFinished());
        assertTrue(asyncResult.isComplete());
        assertNull(asyncResult.getError());

        TransactionInfo transaction = asyncResult.getTransaction();
        assertNotNull(transaction);
        assertEquals(TransactionStatus.OPEN, transaction.getStatus());

        MergeResult result = asyncResult.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());

        assertEquals(1, result.getMergeReport().getConflictCount().longValue());
    }

    /**
     * If merge is called without a transaction, one is created automatically and returned so it can
     * be resumed
     */
    public @Test void mergeConflictAutomaticTransaction() throws Exception {
        createPoiLayer(repo);

        Branch master = repo.branches().getCurrentBranch();
        Branch branch = master.branch("branch1");

        GeogigFeature[] features = testSupport.poiFeatures();
        RevisionFeatureType type = testSupport.poiFeatureType();
        {
            repo.startTransaction();
            master.featureService()
                    .addFeatures(GeogigFeatureCollection.of(type, features[0], features[1]));
            repo.commitSync("add features to master", null);
        }
        {// both add features[0] (fid-1), with different name attribute values
            repo.startTransaction();
            features[0].put("name", "Changed name");
            branch.checkout();
            branch.featureService().addFeatures(
                    GeogigFeatureCollection.of(type, features[0], features[2], features[3]));
            repo.commitSync("add features to branch", null);
        }

        branch = branch.refresh();
        master = master.refresh();

        assertFalse(master.getRepo().isTransactionPresent());
        // call merge without transaction
        AsyncTask<MergeResult> asyncResult = master.merge(branch.getName());
        asyncResult.awaitTermination();

        assertTrue(asyncResult.isFinished());
        assertTrue(asyncResult.isComplete());
        assertNull(asyncResult.getError());

        // a transaction shall have been created for us
        TransactionInfo transaction = asyncResult.getTransaction();
        assertNotNull(transaction);
        // and kept open since there are conflicts
        assertEquals(TransactionStatus.OPEN, transaction.getStatus());

        MergeResult result = asyncResult.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());

        assertEquals(1, result.getMergeReport().getConflictCount().longValue());

        Repo repo = master.getRepo();
        repo.resume(transaction.getId());
    }

    private void assertComplete(AsyncTask<MergeResult> asyncResult) {
        AsyncTaskInfo taskInfo = asyncResult.getInfo();
        assertTrue(asyncResult.isFinished());
        assertTrue(asyncResult.isComplete());
        assertNotNull(taskInfo.getTransaction());
        assertEquals(TransactionStatus.OPEN, taskInfo.getTransaction().getStatus());
    }

    private void assertFailed(AsyncTask<MergeResult> asyncResult) {
        AsyncTaskInfo taskInfo = asyncResult.getInfo();
        assertTrue(asyncResult.isFinished());
        assertFalse(asyncResult.isComplete());
        assertNotNull(taskInfo.getTransaction());
        StatusEnum status = asyncResult.getStatus();
        assertEquals(StatusEnum.FAILED, status);
        assertEquals(TransactionStatus.OPEN, taskInfo.getTransaction().getStatus());
    }
}
