package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.web.client.AsyncTask;
import org.geogig.web.client.Branch;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.client.internal.FeatureServiceApi;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.ConflictDetail;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.geogig.web.model.Value;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Handling conflicts through the API. This test suite assumes {@link MergeTest} works and focus on
 * getting and resolving merge conflicts.
 */
public class ConflictResolutionTest extends AbstractIntegrationTest {

    private Store store;

    private User user;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    private Branch master, branch;

    private GeogigFeature[] poiFeatures;

    private RevisionFeatureType type;

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo("naturalEearth");
        createPoiLayer(repo);
        poiFeatures = testSupport.poiFeatures();
        type = testSupport.poiFeatureType();

        master = repo.branches().getCurrentBranch();
        repo.startTransaction();
        addFeatures(master, type, poiFeatures);
        repo.commit("inital commit").awaitTermination();
        branch = master.branch("branch1");

    }

    private void addFeatures(Branch branch, RevisionFeatureType type, GeogigFeature... features) {
        branch.featureService().addFeatures(GeogigFeatureCollection.of(type, features));
    }

    private void createConflict(GeogigFeature baseFeature, String attName, Object masterValue,
            Object branchValue) {

        GeogigFeature updated = new GeogigFeature(baseFeature);

        repo.startTransaction();
        master.checkout();
        updated.put(attName, masterValue);
        addFeatures(master, type, updated);
        repo.commit(String.format("Update attribute %s of feature %s on master: %s", attName,
                baseFeature.getId(), masterValue)).awaitTermination();

        repo.startTransaction();
        branch.checkout();
        updated.put(attName, branchValue);
        addFeatures(branch, type, updated);
        repo.commit(String.format("Update attribute %s of feature %s on branch: %s", attName,
                baseFeature.getId(), branchValue)).awaitTermination();
    }

    // merge head on to base
    private AsyncTask<MergeResult> merge(Branch base, String head) {
        AsyncTask<MergeResult> task = base.merge(head).awaitTermination();
        assertTrue(task.isFinished());
        assertTrue(task.isComplete());
        MergeResult result = task.getResult();
        assertNotNull(result);
        return task;
    }

    private AsyncTask<MergeResult> mergeExpectConflict(Branch base, String head) {
        AsyncTask<MergeResult> task = merge(base, head);
        TransactionInfo transaction = task.getTransaction();
        assertNotNull(transaction);
        assertEquals(TransactionStatus.OPEN, transaction.getStatus());
        MergeResult result = task.getResult();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMergeReport().getConflictCount().longValue() > 0);
        return task;
    }

    private List<ConflictInfo> getConflicts(@Nullable String pathFilter, final boolean detailed) {
        Iterator<ConflictInfo> conflicts = repo.getConflicts(pathFilter, detailed);
        assertNotNull(conflicts);
        return Lists.newArrayList(conflicts);
    }

    public @Test void getSingleMergeConflict() throws Exception {
        GeogigFeature baseFeature = poiFeatures[0];
        createConflict(baseFeature, "name", "name-master", "name-branch");
        AsyncTask<MergeResult> task = mergeExpectConflict(master, branch.getName());
        MergeResult result = task.getResult();
        assertEquals(1, result.getMergeReport().getConflictCount().longValue());

        repo.resume(task.getTransaction().getId());
        List<ConflictInfo> conflicts = getConflicts(null, false);
        assertEquals(1, conflicts.size());
        ConflictInfo c = conflicts.get(0);
        assertNotNull(c);
        assertNotNull(c.getPath());
        assertNotNull(c.getAncestor());
        assertNotNull(c.getOurs());
        assertNotNull(c.getTheirs());
        assertNull(c.getDetail());
    }

    public @Test void getDetailedConflictInfo() throws Exception {
        GeogigFeature baseFeature1 = poiFeatures[0];
        GeogigFeature baseFeature2 = poiFeatures[1];
        GeogigFeature baseFeature3 = poiFeatures[2];
        createConflict(baseFeature1, "name", "name-master", "name-branch");
        createConflict(baseFeature2, "height", Double.valueOf(0.5), Double.valueOf(0.6));
        createConflict(baseFeature3, "location", geom("POINT(5 5 )"), geom("POINT(6 6 )"));
        AsyncTask<MergeResult> task = mergeExpectConflict(master, branch.getName());
        MergeResult result = task.getResult();
        assertEquals(3, result.getMergeReport().getConflictCount().longValue());

        final boolean DETAILED = true;

        repo.resume(task.getTransaction().getId());
        List<ConflictInfo> conflicts = getConflicts(null, DETAILED);
        assertEquals(3, conflicts.size());

        assertDetailedConflict(baseFeature1, conflicts, "name", "name-master", "name-branch");
        assertDetailedConflict(baseFeature2, conflicts, "height", Double.valueOf(0.5),
                Double.valueOf(0.6));
        assertDetailedConflict(baseFeature3, conflicts, "location", geom("POINT(5 5 )"),
                geom("POINT(6 6 )"));
    }

    /**
     * Using {@link FeatureServiceApi#addFeatures} resolves the conflicts
     */
    public @Test void resolveConflictsWithFeatureService() throws Exception {
        resolveConflictsWithFeatureService(false);
    }

    public @Test void resolveConflictsWithFeatureServiceStageBeforeCommit() throws Exception {
        resolveConflictsWithFeatureService(true);
    }

    private void resolveConflictsWithFeatureService(final boolean stageFirst) throws Exception {
        GeogigFeature baseFeature1 = poiFeatures[0];
        GeogigFeature baseFeature2 = poiFeatures[1];
        GeogigFeature baseFeature3 = poiFeatures[2];
        createConflict(baseFeature1, "name", "name-master", "name-branch");
        createConflict(baseFeature2, "height", Double.valueOf(0.5), Double.valueOf(0.6));
        createConflict(baseFeature3, "location", geom("POINT(5 5)"), geom("POINT(6 6)"));
        AsyncTask<MergeResult> task = mergeExpectConflict(master, branch.getName());
        MergeResult result = task.getResult();
        assertEquals(3, result.getMergeReport().getConflictCount().longValue());

        final boolean DETAILED = true;

        repo.resume(task.getTransaction().getId());
        List<ConflictInfo> conflicts = getConflicts(null, DETAILED);
        assertEquals(3, conflicts.size());
        assertDetailedConflict(baseFeature1, conflicts, "name", "name-master", "name-branch");
        assertDetailedConflict(baseFeature2, conflicts, "height", Double.valueOf(0.5),
                Double.valueOf(0.6));
        assertDetailedConflict(baseFeature3, conflicts, "location", geom("POINT(5 5 )"),
                geom("POINT(6 6 )"));

        FeatureServiceClient featureService = repo.branches().get("master").featureService();
        GeogigFeature resolveFeature1 = new GeogigFeature(baseFeature1);
        GeogigFeature resolveFeature2 = new GeogigFeature(baseFeature2);
        GeogigFeature resolveFeature3 = new GeogigFeature(baseFeature3);

        resolveFeature1.put("name", "name-resolved");
        resolveFeature2.put("height", Double.valueOf(1000));
        resolveFeature3.put("location", geom("POINT(-180 -90)"));

        GeogigFeatureCollection features = GeogigFeatureCollection.of(testSupport.poiFeatureType(),
                resolveFeature1, resolveFeature2, resolveFeature3);

        assertEquals(3, repo.getConflictsCount());
        // this should resolve the conflicts
        featureService.addFeatures(features);
        assertEquals(3, repo.getConflictsCount());
        if (stageFirst) {
            assertTrue(repo.stage().awaitTermination().isComplete());
            assertEquals(0, repo.getConflictsCount());
        }
        // now commit should create a merge commit
        AsyncTask<TransactionInfo> commitTask = repo.commit().awaitTermination();
        assertEquals(StatusEnum.COMPLETE, commitTask.getStatus());
        assertNull(repo.transactionId());
        RevisionCommit mergeCommit = repo.branches().get("master").getCommit();
        assertEquals(2, mergeCommit.getParentIds().size());
    }

    public @Test void resolvePullRequestConflicts() throws Exception {
        final boolean stageFirst = true;
        GeogigFeature baseFeature1 = poiFeatures[0];
        GeogigFeature baseFeature2 = poiFeatures[1];
        GeogigFeature baseFeature3 = poiFeatures[2];
        createConflict(baseFeature1, "name", "name-master", "name-branch");
        createConflict(baseFeature2, "height", Double.valueOf(0.5), Double.valueOf(0.6));
        createConflict(baseFeature3, "location", geom("POINT(5 5)"), geom("POINT(6 6)"));
        AsyncTask<MergeResult> task = mergeExpectConflict(master, branch.getName());
        MergeResult result = task.getResult();
        assertEquals(3, result.getMergeReport().getConflictCount().longValue());

        final boolean DETAILED = true;

        repo.resume(task.getTransaction().getId());
        List<ConflictInfo> conflicts = getConflicts(null, DETAILED);
        assertEquals(3, conflicts.size());
        assertDetailedConflict(baseFeature1, conflicts, "name", "name-master", "name-branch");
        assertDetailedConflict(baseFeature2, conflicts, "height", Double.valueOf(0.5),
                Double.valueOf(0.6));
        assertDetailedConflict(baseFeature3, conflicts, "location", geom("POINT(5 5 )"),
                geom("POINT(6 6 )"));

        FeatureServiceClient featureService = repo.branches().get("master").featureService();
        GeogigFeature resolveFeature1 = new GeogigFeature(baseFeature1);
        GeogigFeature resolveFeature2 = new GeogigFeature(baseFeature2);
        GeogigFeature resolveFeature3 = new GeogigFeature(baseFeature3);

        resolveFeature1.put("name", "name-resolved");
        resolveFeature2.put("height", Double.valueOf(1000));
        resolveFeature3.put("location", geom("POINT(-180 -90)"));

        GeogigFeatureCollection features = GeogigFeatureCollection.of(testSupport.poiFeatureType(),
                resolveFeature1, resolveFeature2, resolveFeature3);

        assertEquals(3, repo.getConflictsCount());
        // this should resolve the conflicts
        featureService.addFeatures(features);
        assertEquals(3, repo.getConflictsCount());
        if (stageFirst) {
            assertTrue(repo.stage().awaitTermination().isComplete());
            assertEquals(0, repo.getConflictsCount());
        }
        // now commit should create a merge commit
        AsyncTask<TransactionInfo> commitTask = repo.commit().awaitTermination();
        assertEquals(StatusEnum.COMPLETE, commitTask.getStatus());
        assertNull(repo.transactionId());
        RevisionCommit mergeCommit = repo.branches().get("master").getCommit();
        assertEquals(2, mergeCommit.getParentIds().size());
        System.err.println(mergeCommit);
    }

    private void assertDetailedConflict(GeogigFeature baseFeature, List<ConflictInfo> conflicts,
            String attName, @Nullable Object oursValue, @Nullable Object theirsValue) {

        Map<String, ConflictInfo> byId = Maps.uniqueIndex(conflicts,
                c -> NodeRef.nodeFromPath(c.getPath()));

        ConflictInfo c1 = byId.get(baseFeature.getId());

        assertNotNull(c1);
        assertNotNull(c1.getPath());
        assertNotNull(c1.getAncestor());
        assertNotNull(c1.getOurs());
        assertNotNull(c1.getTheirs());
        assertNotNull(c1.getDetail());

        ConflictDetail d = c1.getDetail();
        List<String> conflictAttributes = d.getBothEdited();
        RevisionFeature ancestor = d.getAncestor();
        RevisionFeature ours = d.getOurs();
        RevisionFeature theirs = d.getTheirs();

        assertNotNull(conflictAttributes);
        assertNotNull(ancestor);
        assertNotNull(ours);
        assertNotNull(theirs);

        assertEquals(1, conflictAttributes.size());
        assertEquals(attName, conflictAttributes.get(0));

        List<String> attNames = type.getProperties().stream().map(p -> p.getName())
                .collect(Collectors.toList());
        int attIndex = attNames.indexOf(attName);
        assertTrue(attIndex > -1);
        Value value;

        value = ancestor.getValues().get(attIndex);
        Object baseValue = baseFeature.get(attName);
        assertEquals(baseValue, GeogigObjectModelBridge.toObject(value));

        value = ours.getValues().get(attIndex);
        assertEquals(oursValue, GeogigObjectModelBridge.toObject(value));

        value = theirs.getValues().get(attIndex);
        assertEquals(theirsValue, GeogigObjectModelBridge.toObject(value));
    }

    private Geometry geom(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
