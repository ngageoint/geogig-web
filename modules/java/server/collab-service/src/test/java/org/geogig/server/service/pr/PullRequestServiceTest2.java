package org.geogig.server.service.pr;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.feature.FeatureService;
import org.geogig.server.service.rpc.MergeResult;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.test.ServiceTestSupport;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.TransactionResolve;
import org.locationtech.geogig.porcelain.AddOp;
import org.locationtech.geogig.porcelain.ResetOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.WorkingTree;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.locationtech.geogig.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PullRequestServiceTestConfiguration.class)
@DataJpaTest
@Transactional(propagation = Propagation.NEVER)
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class PullRequestServiceTest2 {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired FeatureService featureService;

    private @Autowired PullRequestService service;

    private @Autowired TransactionService transactions;

    private @Autowired BranchAdminService serviceBranchAdmin;

    private User gabe, dave;

    private RepoInfo originRepoInfo, forkRepoInfo;

    private PullRequestRequest request;

    RepoInfo qaqcRepoInfo;

    TestData data_qaqc;

    RepoInfo modifyRepoInfo;

    TestData data_modify;

    RepoInfo modifyRepoInfo2;

    TestData data_modify2;

    RepoInfo delRepoInfo;

    TestData data_del;

    RepoInfo modifyRepoInfo_dbl;

    TestData data_modify_dbl;

    RepoInfo modifyRepoInfo2_dbl;

    TestData data_modify2_dbl;

    RepoInfo delRepoInfo_dbl;

    TestData data_del_dbl;

    ObjectId qaqc_freshState;

    ObjectId modify_freshState;

    ObjectId del_freshState;

    ObjectId modify2_freshState;

    ObjectId modify_dbl_freshState;

    ObjectId modify_dbl2_freshState;

    ObjectId del_dbl_freshState;

    public @After void after() {
        support.getRepos().remove(qaqcRepoInfo.getId());
        support.getRepos().remove(modifyRepoInfo.getId());
        support.getRepos().remove(modifyRepoInfo2.getId());
        support.getRepos().remove(delRepoInfo.getId());
        support.getRepos().remove(modifyRepoInfo_dbl.getId());
        support.getRepos().remove(modifyRepoInfo2_dbl.getId());
        support.getRepos().remove(delRepoInfo_dbl.getId());

    }

    // This sets everything up.
    // We have 7 repos;
    //
    // qaqc -- which is the parent
    // modify -- changes Points/Point1 sp attribute to "modify"
    // modify2 -- changes Points/Point1 sp attribute to "modify2"
    // del -- deletes Points/Point1
    //
    // modify_dbl -- changes Points/Point1 sp attribute to "modify" and on Lines/Lines.1
    // modify2_dbl -- changes Points/Point1 sp attribute to "modify2" and on Lines/Lines.2
    // del_dbl -- deletes Points/Point1 and modify to "del" on Lines/Lines.2
    //
    // All these changes conflict with each other
    public @Before void before() throws Exception {
        gabe = support.createUser("gabe");
        dave = support.createUser("dave");

        qaqcRepoInfo = support.runAs("gabe").createRepo("QAQC");

        data_qaqc = new TestData(support.getRepository(qaqcRepoInfo));
        data_qaqc.loadDefaultData();

        ArrayList<RevCommit> log = newArrayList(data_qaqc.log("master"));
        qaqc_freshState = log.get(0).getId();

        // Repository repo = data_qaqc.getRepo();
        // List<IndexInfo> indexInfos = repo.indexDatabase().getIndexInfos();

        // modify_dbl ------------------------------------
        modifyRepoInfo_dbl = support.runAs("dave").forkAs("gabe", "QAQC", "modifyRepo_dbl");
        data_modify_dbl = new TestData(support.getRepository(modifyRepoInfo_dbl));
        Map<String, SimpleFeature> fs = data_modify_dbl.getFeatures("master");
        SimpleFeature f = fs.get("Points/Point.1");
        f.setAttribute("sp", "modified");
        SimpleFeature f2 = fs.get("Lines/Line.1");
        f2.setAttribute("sp", "modified");
        String m = String.format("change %s and %s on modifyRepo", f.getID(), f2.getID());
        data_modify_dbl.checkout("master").insert(f, f2).add().commit(m);

        // verify
        log = newArrayList(data_modify_dbl.log("master"));
        assertEquals(m, log.get(0).getMessage());
        modify_dbl_freshState = log.get(0).getId();

        fs = data_modify_dbl.getFeatures("master");
        f = fs.get("Points/Point.1");
        assertEquals("modified", f.getAttribute("sp"));
        f = fs.get("Lines/Line.1");
        assertEquals("modified", f.getAttribute("sp"));

        // modify ------------------------------------
        modifyRepoInfo = support.runAs("dave").forkAs("gabe", "QAQC", "modifyRepo");
        data_modify = new TestData(support.getRepository(modifyRepoInfo));
        fs = data_modify.getFeatures("master");
        f = fs.get("Points/Point.1");
        f.setAttribute("sp", "modified");

        m = String.format("change %s on modifyRepo", f.getID());
        data_modify.checkout("master").insert(f, f2).add().commit(m);

        // verify
        log = newArrayList(data_modify.log("master"));
        assertEquals(m, log.get(0).getMessage());
        modify_freshState = log.get(0).getId();

        fs = data_modify.getFeatures("master");
        f = fs.get("Points/Point.1");
        assertEquals("modified", f.getAttribute("sp"));

        // modify2_dbl ------------------------------------
        modifyRepoInfo2_dbl = support.runAs("dave").forkAs("gabe", "QAQC", "modifyRepo2_dbl");
        data_modify2_dbl = new TestData(support.getRepository(modifyRepoInfo2_dbl));
        fs = data_modify2_dbl.getFeatures("master");
        f = fs.get("Points/Point.1");
        f.setAttribute("sp", "modified2");
        f2 = fs.get("Lines/Line.2");
        f2.setAttribute("sp", "modified2");
        m = String.format("change %s and %s on modifyRepo2", f.getID(), f2.getID());
        data_modify2_dbl.checkout("master").insert(f, f2).add().commit(m);

        // verify
        log = newArrayList(data_modify2_dbl.log("master"));
        assertEquals(m, log.get(0).getMessage());
        modify_dbl2_freshState = log.get(0).getId();

        fs = data_modify2_dbl.getFeatures("master");
        f = fs.get("Points/Point.1");
        assertEquals("modified2", f.getAttribute("sp"));
        f = fs.get("Lines/Line.2");
        assertEquals("modified2", f.getAttribute("sp"));

        // modify2 ------------------------------------
        modifyRepoInfo2 = support.runAs("dave").forkAs("gabe", "QAQC", "modifyRepo2");
        data_modify2 = new TestData(support.getRepository(modifyRepoInfo2));
        fs = data_modify2.getFeatures("master");
        f = fs.get("Points/Point.1");
        f.setAttribute("sp", "modified2");
        m = String.format("change %s on modifyRepo2", f.getID());
        data_modify2.checkout("master").insert(f, f2).add().commit(m);

        // verify
        log = newArrayList(data_modify2.log("master"));
        assertEquals(m, log.get(0).getMessage());
        modify2_freshState = log.get(0).getId();

        fs = data_modify2.getFeatures("master");
        f = fs.get("Points/Point.1");
        assertEquals("modified2", f.getAttribute("sp"));

        // del_dbl ------------------------------------
        delRepoInfo_dbl = support.runAs("dave").forkAs("gabe", "QAQC", "delRepo_dbl");
        data_del_dbl = new TestData(support.getRepository(delRepoInfo_dbl));
        fs = data_del_dbl.getFeatures("master");
        f = fs.get("Points/Point.1");
        m = String.format("remove %s on delRepo", f.getID());
        f2 = fs.get("Lines/Line.3");
        f2.setAttribute("sp", "del");
        data_del_dbl.checkout("master").remove(f).insert(f2).add().commit(m);

        log = newArrayList(data_del_dbl.log("master"));
        assertEquals(m, log.get(0).getMessage());
        del_dbl_freshState = log.get(0).getId();

        fs = data_del_dbl.getFeatures("master");
        f = fs.getOrDefault("Points/Point.1", null);
        assertEquals(null, f);
        f = fs.get("Lines/Line.3");
        assertEquals("del", f.getAttribute("sp"));

        // del ------------------------------------
        delRepoInfo = support.runAs("dave").forkAs("gabe", "QAQC", "delRepo");
        data_del = new TestData(support.getRepository(delRepoInfo));
        fs = data_del.getFeatures("master");
        f = fs.get("Points/Point.1");
        m = String.format("remove %s on delRepo", f.getID());

        data_del.checkout("master").remove(f).add().commit(m);

        log = newArrayList(data_del.log("master"));
        assertEquals(m, log.get(0).getMessage());
        del_freshState = log.get(0).getId();

        fs = data_del.getFeatures("master");
        f = fs.getOrDefault("Points/Point.1", null);
        assertEquals(null, f);
    }

    // create a PR (against qaqc)
    public PullRequest createPR(User user, RepoInfo from) {
        RepoInfo to = qaqcRepoInfo;

        PullRequestRequest prRequest = PullRequestRequest.builder()//
                .targetRepo(to.getId())//
                .targetBranch("master")//
                .issuerUser(user.getId())//
                .issuerRepo(from.getId())//
                .issuerBranch("master")//
                .title("My PR - " + from.getIdentity())//
                .description("PR description - " + from.getIdentity())//
                .build();

        PullRequest pr = service.create(prRequest);
        return pr;
    }

    // resets (hard) all the repos to their initial state after @ Begin()
    private void resetAllRepos() {
        assertTrue(data_qaqc.getContext().command(ResetOp.class).setCommit(qaqc_freshState)
                .setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_modify.getContext().command(ResetOp.class).setCommit(modify_freshState)
                .setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_del.getContext().command(ResetOp.class).setCommit(del_freshState)
                .setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_modify2.getContext().command(ResetOp.class).setCommit(modify2_freshState)
                .setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_modify_dbl.getContext().command(ResetOp.class)
                .setCommit(modify_dbl_freshState).setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_modify2_dbl.getContext().command(ResetOp.class)
                .setCommit(modify_dbl2_freshState).setMode(ResetOp.ResetMode.HARD).call());
        assertTrue(data_del_dbl.getContext().command(ResetOp.class).setCommit(del_dbl_freshState)
                .setMode(ResetOp.ResetMode.HARD).call());
    }

    // this does a bunch of tests of various scenarios
    // basically, each one of these calls does
    // 1) PR1 & Merge
    // 2) Sync the 2nd repo (against) qaqc, resolve conflicts with the given features
    // 3) create PR2 & Merge
    //
    // We do a bunch of different scenarios, and resolve conflicts in favour of either #1 or #2.
    // This should test most scenarios.
    //
    // most of these are of the form (with various A & B);
    // A, B, resolve in favor of B
    // A, B, resolve in favor of A
    // B, A, resolve in favor of A
    // B, A, resolve in favor of B
    public @Test void testSyncWorkflow() throws Exception {
        doTwoPRWithSyncWorkflow(modifyRepoInfo, modifyRepoInfo2, data_modify2, data_modify2);
        doTwoPRWithSyncWorkflow(modifyRepoInfo, modifyRepoInfo2, data_modify2, data_modify);

        doTwoPRWithSyncWorkflow(modifyRepoInfo, delRepoInfo, data_del, data_del);
        doTwoPRWithSyncWorkflow(modifyRepoInfo, delRepoInfo, data_del, data_modify);

        doTwoPRWithSyncWorkflow(delRepoInfo, modifyRepoInfo, data_modify, data_modify);
        doTwoPRWithSyncWorkflow(delRepoInfo, modifyRepoInfo, data_modify, data_del);

        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, modifyRepoInfo2, data_modify2, data_modify2);
        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, modifyRepoInfo2, data_modify2, data_modify_dbl);

        doTwoPRWithSyncWorkflow(modifyRepoInfo2, modifyRepoInfo_dbl, data_modify_dbl,
                data_modify_dbl);
        doTwoPRWithSyncWorkflow(modifyRepoInfo2, modifyRepoInfo_dbl, data_modify_dbl, data_modify2);

        doTwoPRWithSyncWorkflow(modifyRepoInfo, delRepoInfo_dbl, data_del_dbl, data_del_dbl);
        doTwoPRWithSyncWorkflow(modifyRepoInfo, delRepoInfo_dbl, data_del_dbl, data_modify);

        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, delRepoInfo_dbl, data_del_dbl, data_del_dbl);
        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, delRepoInfo_dbl, data_del_dbl, data_modify_dbl);

        doTwoPRWithSyncWorkflow(delRepoInfo_dbl, modifyRepoInfo_dbl, data_modify_dbl, data_del_dbl);
        doTwoPRWithSyncWorkflow(delRepoInfo_dbl, modifyRepoInfo_dbl, data_modify_dbl,
                data_modify_dbl);

        doTwoPRWithSyncWorkflow(delRepoInfo_dbl, modifyRepoInfo, data_modify, data_modify);
        doTwoPRWithSyncWorkflow(delRepoInfo_dbl, modifyRepoInfo, data_modify, data_del_dbl);

        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, modifyRepoInfo2_dbl, data_modify2_dbl,
                data_modify2_dbl);
        doTwoPRWithSyncWorkflow(modifyRepoInfo_dbl, modifyRepoInfo2_dbl, data_modify2_dbl,
                data_modify_dbl);

    }

    // 1. PR from first to qaqc and merge -- should be no issues
    // 2. call syncWithConflicts for the 2nd repo (resolve with data from resolveData)
    // 3. issue 2nd PR (should no longer have conflicts)
    // 4. verify conflict is still properly satified
    // 5. resets all the repos to their "fresh" (i.e. same as after @ Begin)
    public void doTwoPRWithSyncWorkflow(RepoInfo first, RepoInfo second, TestData secondData,
            TestData resolveData) throws Exception {

        PullRequest pr1 = createPR(dave, first);
        PullRequestStatus status = merge2("gabe", "QAQC", pr1);

        Map<String, SimpleFeature> resolutionData = resolveData.getFeatures("master");
        Map<String, Map<String, SimpleFeature>> conflicts = syncWithConflicts(dave, second,
                secondData, resolutionData);

        PullRequest pr2 = createPR(dave, second);
        PullRequestStatus status2 = merge2("gabe", "QAQC", pr2);

        // verify conflicts are correctly solved on qaqc
        // verify that the conflicts were correctly resolved
        Map<String, SimpleFeature> fs = data_qaqc.getFeatures("master");
        for (Map.Entry<String, Map<String, SimpleFeature>> layerEntry : conflicts.entrySet()) {
            String layername = layerEntry.getKey();
            Map<String, SimpleFeature> layerConflicts = layerEntry.getValue();

            for (Map.Entry<String, SimpleFeature> entry : layerConflicts.entrySet()) {
                String fid = layername + "/" + entry.getKey();
                SimpleFeature actual = fs.getOrDefault(fid, null);
                SimpleFeature expected = resolutionData.getOrDefault(fid, null);
                assertEquals(expected, actual);
            }
        }
        resetAllRepos();
    }

    // repoInfo - repo to sync (will sync from its parent)
    // data - TestData associated with the repoInfo repo
    // featureForResolution - features to use for resolution (typically a
    // testdata.getFeatures("master"))
    private Map<String, Map<String, SimpleFeature>> syncWithConflicts(User caller,
            RepoInfo repoInfo, TestData data, Map<String, SimpleFeature> featureForResolution)
            throws Exception {
        UUID repo = repoInfo.getId();
        Transaction transaction = transactions.beginTransaction(repo);
        data.resumeTransaction(transaction.getId());
        Task<MergeResult> task = serviceBranchAdmin.sycWithTrackedBranch(caller,
                transaction.getId(), "master");
        MergeResult result = task.getFuture().get();

        // failed and the correct number of conflicts
        assertNotNull(result);
        assertEquals(false, result.isSuccess());
        assertNull(result.getMergeCommit());

        Map<String, Map<String, SimpleFeature>> conflicts = resolveTxConflicts(repoInfo, data,
                transaction.getId(), featureForResolution);

        transactions.commit(caller, transaction, "sync").join();
        data.setTransaction(null);
        assertEquals(Transaction.Status.COMMITTED, transaction.getStatus());

        // verify that the conflicts were correctly resolved
        Map<String, SimpleFeature> fs = data.getFeatures("master");
        for (Map.Entry<String, Map<String, SimpleFeature>> layerEntry : conflicts.entrySet()) {
            String layername = layerEntry.getKey();
            Map<String, SimpleFeature> layerConflicts = layerEntry.getValue();

            for (Map.Entry<String, SimpleFeature> entry : layerConflicts.entrySet()) {
                String fid = layername + "/" + entry.getKey();
                SimpleFeature actual = fs.getOrDefault(fid, null);
                SimpleFeature expected = featureForResolution.getOrDefault(fid, null);
                assertEquals(expected, actual);
            }
        }
        return conflicts;
    }

    // resolves the conflicts on the TX with features from data
    // this will do the insert/remove and stage
    // at the end of this, the will be no remaining conflicts on the TX.
    // returns the layer-> {fid->feature} for the resolutions
    public Map<String, Map<String, SimpleFeature>> resolveTxConflicts(RepoInfo repoInfo,
            TestData repodata, UUID tx, Map<String, SimpleFeature> featureForResolution) {
        Map<String, Map<String, SimpleFeature>> conflicts = getConflicts(repoInfo, tx,
                featureForResolution);

        List<String> conflictFids = new ArrayList<>();
        // insert (or delete) resolutions
        for (Map.Entry<String, Map<String, SimpleFeature>> layerEntry : conflicts.entrySet()) {
            String layername = layerEntry.getKey();
            Map<String, SimpleFeature> layerConflicts = layerEntry.getValue();

            for (Map.Entry<String, SimpleFeature> entry : layerConflicts.entrySet()) {
                conflictFids.add(layername + "/" + entry.getKey());
                if (entry.getValue() == null) { // delete
                    repodata.remove(layername + "/" + entry.getKey());
                } else { // insert to working head
                    repodata.insert(entry.getValue());
                }
            }
        }
        /// assertEquals(conflictFids.size(), result.getReport().getConflicts());

        AddOp addop = repodata.getContext().command(AddOp.class);

        // stage changes (important because if you insert a feature that isn't different, it doesn't
        // get marked as resolved)
        for (String fid : conflictFids) {
            addop.addPattern(fid);
        }

        WorkingTree tree = addop.call();

        // verify there aren't any remaining conflicts
        Map<String, Map<String, SimpleFeature>> remainingConflicts = getConflicts(repoInfo, tx,
                featureForResolution);
        int nconflicts = 0;

        for (Map<String, SimpleFeature> layerConflicts : remainingConflicts.values()) {
            nconflicts += layerConflicts.size();
        }
        assertEquals(0, nconflicts);
        return conflicts;
    }

    public GeogigTransaction getTransaction(UUID transactionId, RepoInfo repositoryInfo) {
        Repository repository = support.getRepository(repositoryInfo);
        Optional<GeogigTransaction> tx = repository.context().command(TransactionResolve.class)
                .setId(transactionId).call();
        return tx.get();
    }

    // layername - > {fid->feature}
    // repositoryInfo -- TX's repo
    // tx -- tx that should be in conflict
    // data -- where to get FeatureData From (by ID)
    //
    // This goes to the given Transaction (defined by repositoryInfo/tx)
    // and gets a list of conflicts in each layer.
    // using the layer/FID, it then gets those features from the given TestData.
    // You can then use these features to resolve the conflicts.
    // note - NULL feature means to delete.
    private Map<String, Map<String, SimpleFeature>> getConflicts(RepoInfo repositoryInfo, UUID tx,
            Map<String, SimpleFeature> fs) {
        User u = support.getUser(repositoryInfo.getOwnerId());
        List<LayerInfo> layers = featureService.getLayers(u.getIdentity(),
                repositoryInfo.getIdentity(), tx, "HEAD");

        // Map<String, SimpleFeature> fs = data.getFeatures("master");
        Map<String, Map<String, SimpleFeature>> result = new HashMap<>();
        for (LayerInfo layer : layers) {
            List<String> conflicts = getConflicts(repositoryInfo, layer.getName(), tx);

            Map<String, SimpleFeature> layerResult = new HashMap<>();
            conflicts.forEach(fid -> layerResult.put(fid,
                    fs.getOrDefault(layer.getName() + "/" + fid, null)));
            result.put(layer.getName(), layerResult);
        }

        return result;
    }

    // get the underlying conflicts in a TX (single layer)
    // returns the "bad" fids
    private List<String> getConflicts(RepoInfo repositoryInfo, String layername, UUID tx) {
        Context cx = getTransaction(tx, repositoryInfo);
        GeogigFeatureCollection fc = featureService.getConflictingFeatures(cx, layername,
                new FeatureQuery());

        ArrayList<String> result = new ArrayList<>();
        fc.forEachRemaining(f -> result.add(f.getId()));
        return result;
    }

    private PullRequestStatus merge2(String user, String repo, PullRequest pr)
            throws ExecutionException {
        Task<PullRequestStatus> task = service.merge(user, repo, pr.getId(), "title", "message");

        PullRequestStatus status;
        try {
            status = task.getFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        PullRequest postMerge = service.getOrFail(pr.getRepositoryId(), pr.getId());
        assertEquals(PullRequest.Status.MERGED, postMerge.getStatus());
        assertNotNull(postMerge.getUpdatedAt());
        assertNotNull(postMerge.getClosedByUserId());
        return status;
    }

}
