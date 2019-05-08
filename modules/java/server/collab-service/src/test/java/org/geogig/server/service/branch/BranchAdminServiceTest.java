package org.geogig.server.service.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.test.TestData.line1;
import static org.locationtech.geogig.test.TestData.line2;
import static org.locationtech.geogig.test.TestData.line3;
import static org.locationtech.geogig.test.TestData.point1;
import static org.locationtech.geogig.test.TestData.point1_modified;
import static org.locationtech.geogig.test.TestData.point2;
import static org.locationtech.geogig.test.TestData.point3;
import static org.locationtech.geogig.test.TestData.poly2;
import static org.locationtech.geogig.test.TestData.poly3;
import static org.locationtech.geogig.test.TestData.poly4;

import java.util.Map;
import java.util.UUID;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.rpc.MergeResult;
import org.geogig.server.service.rpc.PullArgs;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.test.ServiceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;
import org.locationtech.geogig.porcelain.NothingToCommitException;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterators;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BranchAdminServiceTestConfiguration.class)
@DataJpaTest
@Transactional(propagation = Propagation.NEVER)
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class BranchAdminServiceTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired RepositoryRPCService rpc;

    private @Autowired TransactionService transactions;

    private @Autowired BranchAdminService service;

    private User gabe, dave;

    private RepoInfo originRepoInfo, forkRepoInfo;

    /**
     * <pre>
     * <code>
     * 
     *  master o-----------------------------------------
     *            (initial commit has Point.1, Line.1, Polygon.1)  
     * </code>
     * </pre>
     */
    private TestData origin;

    /**
     * <pre>
     * <code>  forkBranch  remove Line.1   add Polygon.4   modify Point.1
     *           ---------------o---------------o----------------o
     *          /                                        
     *  master o-----------------------------------------
     *            (initial commit has Point.1, Line.1, Polygon.1)  
     * </code>
     * </pre>
     */
    private TestData clone;

    public @After void after() {
    }

    public @Before void before() throws Exception {
        gabe = support.createUser("gabe");
        dave = support.createUser("dave");
        originRepoInfo = support.runAs("gabe").createRepo("repo");

        origin = new TestData(support.getRepository(originRepoInfo));
        origin.loadDefaultData();
        RevCommit commonAncestor = Iterators.getLast(origin.log("master"));
        origin.resetHard(commonAncestor.getId());

        forkRepoInfo = support.runAs("dave").forkAs("gabe", originRepoInfo.getIdentity(),
                "forkName");
        assertNotNull(forkRepoInfo);

        clone = new TestData(support.getRepository(forkRepoInfo));

        SimpleFeature point1Modified = TestData.clone(point1_modified);
        point1Modified.setAttribute("sp", "modified by clone");

        clone.branchAndCheckout("forkBranch")//
                .resetHard(commonAncestor.getId())//
                .remove(line1).add().commit("remove line1")//
                .insert(poly4).add().commit("add poly 4")//
                .insert(point1Modified).add().commit("modify point1");
    }

    public @Test void getBranchWithRemote() {
        String branchName = "master";
        UUID txId = null;
        String user = dave.getIdentity();
        String repo = forkRepoInfo.getIdentity();
        Branch branch = service.getBranch(user, repo, branchName, txId);
        assertEquals("refs/heads/master", branch.getRemoteBranch().orElse(null));
    }

    public @Test(expected = NothingToCommitException.class) void sycWithTrackedBranchUpToDate()
            throws Exception {

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        service.sycWithTrackedBranch(dave, transaction.getId(), "master");
    }

    public @Test(expected = IllegalArgumentException.class) void sycWithTrackedBranchNoTrackedBranch()
            throws Exception {

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        service.sycWithTrackedBranch(dave, transaction.getId(), "forkBranch");
    }

    public @Test void testSyncTrackingBranchFastForward() throws Exception {
        final ObjectId upstreamCommitId = origin.checkout("master").insert(line2, point2, poly2)
                .add().commit("changes upstream").getRef("master").getObjectId();

        assertEquals(3, clone.getFeatureNodes("master").size());
        assertEquals(6, origin.getFeatureNodes("master").size());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        assertNotNull(result.getMergeCommit());

        transactions.commit(dave, transaction, "not needed").join();

        Map<String, NodeRef> featureNodes = clone.getFeatureNodes("master");
        assertEquals(6, featureNodes.size());

        assertEquals("changes upstream", result.getMergeCommit().getMessage());
        assertEquals(upstreamCommitId, result.getMergeCommit().getId());
    }

    public @Test void testSyncTrackingBranchNoFastForward() throws Exception {
        final ObjectId upstreamCommitId = origin.checkout("master").insert(line2, point2, poly2)
                .add().commit("changes upstream").getRef("master").getObjectId();

        final ObjectId downstreamCommitId = clone.checkout("master").insert(point3, line3, poly3)
                .add().commit("changes downstream").getRef("master").getObjectId();

        assertEquals(6, clone.getFeatureNodes("master").size());
        assertEquals(6, origin.getFeatureNodes("master").size());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        RevCommit mergeCommit = result.getMergeCommit();
        assertNotNull(mergeCommit);

        transactions.commit(dave, transaction, "not needed").join();

        Map<String, NodeRef> featureNodes = clone.getFeatureNodes("master");
        assertEquals(9, featureNodes.size());

        String commitMessage = String.format("Synchronize changes from tracked branch %s:%s:%s",
                gabe.getIdentity(), originRepoInfo.getIdentity(), "refs/heads/master");
        assertEquals(commitMessage, mergeCommit.getMessage());
        assertEquals(downstreamCommitId, mergeCommit.getParentIds().get(0));
        assertEquals(upstreamCommitId, mergeCommit.getParentIds().get(1));
    }

    public @Test void testSynchronizeUpstreamTrackingBranchConflicts() throws Exception {
        createConflicts(line2, point2, poly2);

        assertEquals(6, clone.getFeatureNodes("master").size());
        assertEquals(6, origin.getFeatureNodes("master").size());

        Branch branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master",
                null);
        assertEquals(3, branch.getCommitsBehind().get().intValue());
        assertEquals(3, branch.getCommitsAhead().get().intValue());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getMergeCommit());
        assertNotNull(result.getReport());
        MergeScenarioReport report = result.getReport();
        assertEquals(3, report.getConflicts());

        String repo = forkRepoInfo.getIdentity();
        UUID txId = transaction.getId();
        assertEquals(1, rpc.countConflicts("dave", repo, txId, "Points"));
        assertEquals(1, rpc.countConflicts("dave", repo, txId, "Lines"));
        assertEquals(1, rpc.countConflicts("dave", repo, txId, "Polygons"));
    }

    public @Test void syncUpstreamTrackingBranchResolveConflicts() throws Exception {
        // conflicts
        createConflicts(line2, point2, poly2);

        // non conflicts upstream
        origin.insert(point3, line3, poly3).add().commit("non conflicts upstream");

        Branch branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master",
                null);
        assertEquals(4, branch.getCommitsBehind().get().intValue());
        assertEquals(3, branch.getCommitsAhead().get().intValue());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertFalse(result.isSuccess());
        MergeScenarioReport report = result.getReport();
        assertEquals(3, report.getConflicts());

        String repo = forkRepoInfo.getIdentity();
        UUID txId = transaction.getId();
        // resolve conflicts by simply staging what's currently on the WORK_HEAD
        rpc.stage("dave", repo, txId, null).getFuture().get();
        assertEquals(0, rpc.countConflicts("dave", repo, txId, null));
        // verify stage solved conflicts with the local changes
        Map<String, SimpleFeature> staged = clone.resumeTransaction(txId)
                .getFeatures(Ref.STAGE_HEAD, null);
        assertEquals("modified on fork", staged.get("Points/Point.2").getAttribute("sp"));
        assertEquals("modified on fork", staged.get("Lines/Line.2").getAttribute("sp"));
        assertEquals("modified on fork", staged.get("Polygons/Polygon.2").getAttribute("sp"));

        clone.exitFromTransaction();

        // now commit the transaction, should create a merge commit with our resolved state, and
        // contain the upstream commits
        transactions.commit(dave, transaction, "Resolve sync conflicts").join();
        assertEquals(Transaction.Status.COMMITTED, transactions.getOrFail(txId).getStatus());

        Map<String, SimpleFeature> live = clone.getFeatures(Ref.HEAD, null);
        assertEquals("modified on fork", live.get("Points/Point.2").getAttribute("sp"));
        assertEquals("modified on fork", live.get("Lines/Line.2").getAttribute("sp"));
        assertEquals("modified on fork", live.get("Polygons/Polygon.2").getAttribute("sp"));
        assertTrue(live.containsKey("Points/Point.3"));
        assertTrue(live.containsKey("Lines/Line.3"));
        assertTrue(live.containsKey("Polygons/Polygon.3"));
    }

    public @Test void synchronizeUpstreamTrackingBranchWithParentMergeCommit() throws Exception {

        origin.branchAndCheckout("newbranch")//
                .insert(line2).add().commit("line2")//
                .insert(point2).add().commit("point2")//
                .insert(poly2).add().commit("poly2")//
                .checkout("master")//
                .mergeNoFF("newbranch", "merge branch newbranch");

        assertEquals(6, origin.getFeatureNodes("master").size());
        assertEquals(3, clone.getFeatureNodes("master").size());

        Branch branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master",
                null);
        assertEquals(4, branch.getCommitsBehind().get().intValue());
        assertEquals(0, branch.getCommitsAhead().get().intValue());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMergeCommit());
        assertNotNull(result.getReport());
        MergeScenarioReport report = result.getReport();
        assertEquals(0, report.getConflicts());
        assertEquals(3, report.getUnconflictedFeatures());

        assertEquals("Sync should have been a fast forward merge", "merge branch newbranch",
                result.getMergeCommit().getMessage());

        transactions.commit(dave, transaction, "not needed").join();

        branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master", null);
        assertEquals(0, branch.getCommitsBehind().get().intValue());
        assertEquals(0, branch.getCommitsAhead().get().intValue());

        assertEquals(6, clone.getFeatureNodes("master").size());
    }

    public @Test void syncUpstreamTrackingBranchWithParentMergeCommitAndLocalChange()
            throws Exception {

        final RevCommit originMergeCommit = origin.branchAndCheckout("newbranch")//
                .insert(line2).add().commit("line2")//
                .insert(point2).add().commit("point2")//
                .insert(poly2).add().commit("poly2")//
                .checkout("master")//
                .mergeNoFF("newbranch", "merge branch newbranch")//
                .log("HEAD").next();
        assertEquals("merge branch newbranch", originMergeCommit.getMessage());

        SimpleFeature change = TestData.clone(point1);
        change.setAttribute("sp", "changed downstream");
        final RevCommit localChangeCommit = clone.checkout("master").insert(change).add()
                .commit("commit change downstream").log("HEAD").next();
        assertEquals("commit change downstream", localChangeCommit.getMessage());

        Branch branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master",
                null);
        assertEquals(4, branch.getCommitsBehind().get().intValue());
        assertEquals(1, branch.getCommitsAhead().get().intValue());

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());

        Task<MergeResult> task = service.sycWithTrackedBranch(dave, transaction.getId(), "master");

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMergeCommit());
        assertNotNull(result.getReport());
        MergeScenarioReport report = result.getReport();
        assertEquals(0, report.getConflicts());
        assertEquals(3, report.getUnconflictedFeatures());
        assertEquals("Synchronize changes from tracked branch gabe:repo:refs/heads/master",
                result.getMergeCommit().getMessage());

        transactions.commit(dave, transaction, "not needed").join();

        branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master", null);
        assertEquals(0, branch.getCommitsBehind().get().intValue());
        assertEquals(
                "should be 2 commits ahead, the previous one and the one resulting from merging local with upstream",
                2, branch.getCommitsAhead().get().intValue());

        assertEquals(6, clone.getFeatureNodes("master").size());
    }

    public @Test void testSyncExplicitRemoteBranchNoFastForward() throws Exception {
        User forkOwner = dave;
        RepoInfo forkOfForkInfo = support.runAs("dave").forkAs("dave", forkRepoInfo.getIdentity(),
                "forkOfFork");

        TestData forkOfFork = new TestData(support.getRepository(forkOfForkInfo));

        final ObjectId upstreamCommitId = origin.checkout("master").insert(line2, point2, poly2)
                .add().commit("changes upstream").getRef("master").getObjectId();

        final ObjectId downstreamCommitId = forkOfFork.checkout("master")
                .insert(point3, line3, poly3).add().commit("changes downstream").getRef("master")
                .getObjectId();

        assertEquals(6, forkOfFork.getFeatureNodes("master").size());
        assertEquals(6, origin.getFeatureNodes("master").size());

        Transaction transaction = transactions.beginTransaction(forkOfForkInfo.getId());

        PullArgs args = PullArgs.builder()//
                .remoteRepo(originRepoInfo.getId())//
                .remoteBranch("master")//
                .commitMessage("provided commit message")//
                .targetRepo(forkOfForkInfo.getId())//
                .targetBranch("master")//
                .build();

        Task<MergeResult> task = service.syncWithRemoteBranch(dave, transaction.getId(), "master",
                args);

        MergeResult result = task.getFuture().get();
        assertNotNull(result);
        RevCommit mergeCommit = result.getMergeCommit();
        assertNotNull(mergeCommit);

        transactions.commit(forkOwner, transaction, "not needed").join();

        Map<String, NodeRef> featureNodes = forkOfFork.getFeatureNodes("master");
        assertEquals(9, featureNodes.size());

        assertEquals("provided commit message", mergeCommit.getMessage());
        assertEquals(downstreamCommitId, mergeCommit.getParentIds().get(0));
        assertEquals(upstreamCommitId, mergeCommit.getParentIds().get(1));
    }

    public @Test void testSyncExplicitBranchResolveConflicts() throws Exception {
        // conflicts
        createConflicts(line2, point2, poly2);

        // non conflicts upstream
        origin.insert(point3, line3, poly3).add().commit("non conflicts upstream");

        Branch branch = service.getBranch(dave.getIdentity(), forkRepoInfo.getIdentity(), "master",
                null);
        assertEquals(4, branch.getCommitsBehind().get().intValue());
        assertEquals(3, branch.getCommitsAhead().get().intValue());

        final Transaction tx = transactions.beginTransaction(forkRepoInfo.getId());

        PullArgs args = PullArgs.builder().remoteRepo(originRepoInfo.getId()).remoteBranch("master")
                .targetRepo(forkRepoInfo.getId()).targetBranch("master")
                .commitMessage("should not be used due to conflicts when pulling").build();

        MergeResult result = service.syncWithRemoteBranch(dave, tx.getId(), "master", args)
                .getFuture().get();

        assertFalse(result.isSuccess());
        assertEquals(3, result.getReport().getConflicts());

        // resolve conflicts by simply staging what's currently on the WORK_HEAD
        rpc.stage("dave", forkRepoInfo.getIdentity(), tx.getId(), null).getFuture().get();
        assertEquals(0, rpc.countConflicts("dave", forkRepoInfo.getIdentity(), tx.getId(), null));

        // now commit the transaction, should create a merge commit with our resolved state, and
        // contain the upstream commits
        transactions.commit(dave, tx, "Resolve sync conflicts").join();
        assertEquals(Transaction.Status.COMMITTED, transactions.getOrFail(tx.getId()).getStatus());

        ObjectDatabase odb = clone.getRepo().objectDatabase();
        RevCommit mergeCommit = odb.getCommit(clone.getRef("master").getObjectId());
        assertEquals(2, mergeCommit.getParentIds().size());
        assertEquals("Resolve sync conflicts", mergeCommit.getMessage());
    }

    private void createConflicts(SimpleFeature... features) {
        for (SimpleFeature base : features) {
            SimpleFeature toOrigin = TestData.clone(base);
            toOrigin.setAttribute("sp", "modified on origin");

            origin.checkout("master").insert(toOrigin).add()
                    .commit(String.format("change %s on origin", base.getID()));

            SimpleFeature toClone = TestData.clone(base);
            toClone.setAttribute("sp", "modified on fork");

            clone.checkout("master").insert(toClone).add()
                    .commit(String.format("change %s on clone", base.getID()));
        }
    }
}
