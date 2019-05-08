package org.geogig.server.service.feature;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.geogig.server.model.AsyncTask;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.Transaction.Status;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.test.ServiceTestSupport;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.DataUtilities;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FeatureServiceTestConfiguration.class)
@DataJpaTest
@Transactional(propagation = Propagation.NEVER) // so entities are seen by other threads
public class FeatureServiceTest {

    private @Autowired FeatureService service;

    private @Autowired TransactionService txService;

    private @Autowired AsyncTasksService asyncService;

    public @Rule @Autowired ServiceTestSupport support;

    /**
     * <pre>
     * <code>
     *             (adds Points/2, Lines/2, Polygons/2)
     *    branch1 o-------------------------------------
     *           /                                      \
     *          /                                        \  no ff merge
     *  master o------------------------------------------o-----------------o
     *          \  (initial commit has                                     / no ff merge
     *           \     Points/1, Lines/1, Polygons/1)                     /
     *            \                                                      /
     *             \                                                    /
     *     branch2  o--------------------------------------------------
     *             (adds Points/3, Lines/3, Polygons/3)
     *
     * </code>
     * </pre>
     */
    private RepoInfo repo, fork;

    private User user;

    private TestData repo1Support;

    private String userName, repoName, forkName;

    private Transaction transaction;

    private SimpleFeatureType poiType;

    private RevisionFeatureType poiRevType;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() throws Exception {
        user = support.createUser("user");

        support.runAs(user.getIdentity());

        repo = support.createRepo("repo");
        repo1Support = new TestData(support.getRepository(repo));
        repo1Support.loadDefaultData();

        fork = support.forkAs(user.getIdentity(), repo.getIdentity(), "fork");
        userName = user.getIdentity();
        repoName = repo.getIdentity();
        forkName = fork.getIdentity();

        poiType = DataUtilities.createType("Poi", TestData.pointsTypeSpec);
        poiRevType = GeoToolsDomainBridge.toFeatureType(poiType);
        transaction = txService.beginTransaction(repo.getId());
    }

    private void commit(Transaction tx, String commitMessage) throws Exception {
        RepoInfo repo = support.getRepositoryInfo(tx.getRepositoryId());
        User user = support.getUser(repo.getOwnerId());
        String u = user.getIdentity();
        String r = repo.getIdentity();
        AsyncTask task = txService.commitTransaction(u, r, tx.getId(), commitMessage);
        await().atMost(10, TimeUnit.MINUTES).until(() -> taskFinished(task));
        Task<?> internalTask = asyncService.getTask(task.getId()).get();
        internalTask.getFuture().get();
        Transaction txState = txService.getOrFail(tx.getId());
        Status status = txState.getStatus();
        assertEquals(Status.COMMITTED, status);
    }

    private boolean taskFinished(AsyncTask task) {
        Task<?> t = asyncService.getTask(task.getId())
                .orElseThrow(() -> new NoSuchElementException("Task not found " + task));
        Future<?> future = t.getFuture();
        boolean done = future != null && future.isDone();
        if (done) {
            log.info("task finished: " + t.getTaskInfo());
        }
        return done;
    }

    public @Test(expected = NoSuchElementException.class) void createLayerInvalidTransaction()
            throws Exception {

        service.create(userName, repoName, UUID.randomUUID(), poiRevType);
    }

    public @Test(expected = IllegalArgumentException.class) void createLayerExistingLayer()
            throws Exception {
        RevisionFeatureType existing = GeoToolsDomainBridge.toFeatureType(TestData.pointsType);
        service.create(userName, repoName, transaction.getId(), existing);
    }

    public @Test void createLayer() throws Exception {
        LayerInfo layer = service.create(userName, repoName, transaction.getId(), poiRevType);
        assertNotNull(layer);
        assertEquals(poiRevType.getName(), layer.getName());
        assertNotNull(layer.getBounds());
        assertEquals(0L, layer.getSize().longValue());
        RevisionFeatureType type = layer.getType();
        assertNotNull(type);
        assertNotNull(type.getId());
        poiRevType.setId(type.getId());
        assertEquals(poiRevType, type);
        getLayerExpectNoSuchElementException(layer.getName(), null, null);
        getLayerExpectNoSuchElementException(layer.getName(), Ref.HEAD, null);
        getLayerExpectNoSuchElementException(layer.getName(), Ref.WORK_HEAD, null);
        getLayerExpectNoSuchElementException(layer.getName(), Ref.STAGE_HEAD, null);
        getLayerExpectNoSuchElementException(layer.getName(), "master", null);

        assertNotNull(getRepoLayer(layer.getName(), Ref.WORK_HEAD, transaction));
        getLayerExpectNoSuchElementException(layer.getName(), Ref.STAGE_HEAD, transaction);
        getLayerExpectNoSuchElementException(layer.getName(), "master", transaction);

        commit(transaction, "created poi");
        RevCommit headCommit = repo1Support.log(Ref.HEAD).next();
        assertEquals("created poi", headCommit.getMessage());

        assertNotNull(getRepoLayer(layer.getName(), Ref.WORK_HEAD, null));
        assertNotNull(getRepoLayer(layer.getName(), Ref.STAGE_HEAD, null));
        assertNotNull(getRepoLayer(layer.getName(), Ref.HEAD, null));
    }

    private void getLayerExpectNoSuchElementException(String layer, String head, Transaction tx) {
        try {
            getRepoLayer(layer, head, tx);
            fail("Expected NSE");
        } catch (NoSuchElementException e) {
            assertThat(e.getMessage(), Matchers.containsString("Layer " + layer + " not found"));
        }
    }

    private LayerInfo getRepoLayer(String layer, String head, Transaction tx) {
        return service.getLayer(userName, repoName, layer, head, tx == null ? null : tx.getId());
    }

    @Ignore
    public @Test final void testGetLayer() {
        fail("Not yet implemented");
    }

    @Ignore
    public @Test final void testGetLayers() {
        fail("Not yet implemented");
    }

    @Ignore
    public @Test final void testGetFeaturesStringStringStringFeatureQueryUUID() {
        fail("Not yet implemented");
    }

    @Ignore
    public @Test final void testGetFeaturesUUIDUUIDStringFeatureQuery() {
        fail("Not yet implemented");
    }

    @Ignore
    public @Test final void testGetConflictingFeatures() {
        fail("Not yet implemented");
    }

    @Ignore
    public @Test final void testGetFeaturesContextStringFeatureQuery() {
        fail("Not yet implemented");
    }

    public @Test final void insertAssignsSensibleFeatureIds() {
        UUID txId = transaction.getId();

        RevisionFeatureType type = GeoToolsDomainBridge.toFeatureType(TestData.linesType);
        String layer = type.getName();
        GeogigFeature f1 = GeoToolsDomainBridge.toFeature(TestData.line1);
        GeogigFeature f2 = GeoToolsDomainBridge.toFeature(TestData.line2);
        GeogigFeature f3 = GeoToolsDomainBridge.toFeature(TestData.line3);
        f1.setId(null);
        f2.setId(null);
        f3.setId(null);
        f1.put("sp", "f1");
        f2.put("sp", "f2");
        f3.put("sp", "f3");

        GeogigFeatureCollection collection = GeogigFeatureCollection.of(type, f1, f2, f3);

        repo1Support.resumeTransaction(txId);
        Map<String, SimpleFeature> beforeFeatures = repo1Support.getFeatures(Ref.WORK_HEAD, layer);

        service.insert(userName, repoName, layer, collection, txId);

        Map<String, SimpleFeature> afterFeatures = repo1Support.getFeatures(Ref.WORK_HEAD, layer);
        Set<String> newPaths = Sets.difference(afterFeatures.keySet(), beforeFeatures.keySet());
        assertEquals(3, newPaths.size());
        List<String> newFids = newPaths.stream().map(s -> s.substring((layer + "/").length()))
                .collect(Collectors.toList());
        for (String newfid : newFids) {
            assertTrue("FID too long: " + newfid, newfid.length() < 10);
        }
    }

    @Ignore
    public @Test final void testUpdate() {
        fail("Not yet implemented");
    }

    public @Test final void deleteFeaturesById() throws Exception {
        String layer = TestData.pointsType.getTypeName();
        UUID txId = transaction.getId();
        FeatureFilter filter = new FeatureFilter();
        String fid1 = TestData.point1.getID();
        String fid2 = TestData.point2.getID();
        String fid3 = TestData.point3.getID();

        repo1Support.resumeTransaction(txId);
        Map<String, SimpleFeature> beforeFeatures = repo1Support.getFeatures(Ref.WORK_HEAD, layer);
        assertTrue(beforeFeatures.containsKey(String.format("%s/%s", layer, fid1)));
        assertTrue(beforeFeatures.containsKey(String.format("%s/%s", layer, fid3)));

        filter.addFeatureIdsItem(fid1).addFeatureIdsItem(fid3);
        service.delete(userName, repoName, layer, txId, filter);

        Map<String, SimpleFeature> afterFeatures = repo1Support.getFeatures(Ref.WORK_HEAD, layer);
        assertFalse(afterFeatures.containsKey(String.format("%s/%s", layer, fid1)));
        assertFalse(afterFeatures.containsKey(String.format("%s/%s", layer, fid3)));
        assertTrue(afterFeatures.containsKey(String.format("%s/%s", layer, fid2)));

        String msg = String.format("Delte %s, %s", fid1, fid3);
        commit(transaction, msg);

        repo1Support.exitFromTransaction();
        afterFeatures = repo1Support.getFeatures(Ref.HEAD, layer);
        assertFalse(afterFeatures.containsKey(String.format("%s/%s", layer, fid1)));
        assertFalse(afterFeatures.containsKey(String.format("%s/%s", layer, fid3)));
        assertTrue(afterFeatures.containsKey(String.format("%s/%s", layer, fid2)));
    }

    @Ignore
    public @Test final void testDeleteLayer() {
        fail("Not yet implemented");
    }

}
