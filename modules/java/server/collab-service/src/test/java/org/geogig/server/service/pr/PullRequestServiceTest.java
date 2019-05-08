package org.geogig.server.service.pr;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.geogig.server.model.PullRequestStatus.MergeableStatus.CHECKING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.PullRequestStatus.MergeableStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.feature.FeatureService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.test.ServiceTestSupport;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.ResolveCommit;
import org.locationtech.geogig.repository.Repository;
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
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PullRequestServiceTestConfiguration.class)
@DataJpaTest
@Transactional(propagation = Propagation.NEVER)
@TestPropertySource(inheritProperties = true, properties = {
        "spring.main.allow-bean-definition-overriding=true", })
public class PullRequestServiceTest {

    public @Rule @Autowired ServiceTestSupport support;

    private @Autowired FeatureService featureService;

    private @Autowired TransactionService transactions;

    private @Autowired BranchAdminService branches;

    private @Autowired PullRequestService service;

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
     * <code>  issuerBranch  remove Line.1   add Polygon.4   modify Point.1
     *           ---------------o---------------o----------------o
     *          /                                        
     *  master o-----------------------------------------
     *            (initial commit has Point.1, Line.1, Polygon.1)  
     * </code>
     * </pre>
     */
    private TestData clone;

    private PullRequestRequest request;

    private @Autowired Listener listener;

    public @After void after() {
        listener.clear();
    }

    public @Before void before() throws Exception {
        gabe = support.createUser("gabe");
        dave = support.createUser("dave");
        originRepoInfo = support.runAs("gabe").createRepo("origin");

        origin = new TestData(support.getRepository(originRepoInfo));
        origin.loadDefaultData();
        RevCommit commonAncestor = Iterators.getLast(origin.log("master"));
        origin.resetHard(commonAncestor.getId());

        forkRepoInfo = support.runAs("dave").fork("gabe", "origin");
        assertNotNull(forkRepoInfo);

        clone = new TestData(support.getRepository(forkRepoInfo));
        TestData.point1_modified.setAttribute("sp", "modified by clone");
        clone.branchAndCheckout("issuerBranch")//
                .resetHard(commonAncestor.getId())//
                .remove(TestData.line1).add().commit("remove line1")//
                .insert(TestData.poly4).add().commit("add poly 4")//
                .insert(TestData.point1_modified).add().commit("modify point1");

        request = PullRequestRequest.builder()//
                .targetRepo(originRepoInfo.getId())//
                .targetBranch("master")//
                .issuerUser(dave.getId())//
                .issuerRepo(forkRepoInfo.getId())//
                .issuerBranch("issuerBranch")//
                .title("My PR")//
                .description("PR description")//
                .build();
    }

    public @Test void testCreate() {
        PullRequest created = service.create(request);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(created, service.getOrFail(originRepoInfo.getId(), created.getId()));
        assertEquals(PullRequest.Status.OPEN, created.getStatus());
    }

    public @Test void testCreateLaunchesTestMerge()
            throws InterruptedException, ExecutionException {

        PullRequest pr = service.create(request);
        assertNotNull(listener.initEvent);
        assertNotNull(listener.initEvent.getPr());

        await().atMost(1, SECONDS).until(() -> !listener.prepareStart.isEmpty());

        assertEquals(1, listener.prepareStart.size());
        assertEquals(pr, listener.prepareStart.get(0).getRequest());
        PullRequestStatus status;

        await().atMost(3, SECONDS).until(() -> !status(pr).getMergeable().equals(CHECKING));
        status = status(pr);

        assertEquals(1, listener.prepareEnd.size());
        assertEquals(MergeableStatus.MERGEABLE, status.getMergeable());
        assertEquals(Sets.newHashSet("Points", "Lines", "Polygons"),
                Sets.newHashSet(status.getAffectedLayers()));
    }

    private PullRequestStatus status(PullRequest pr)
            throws InterruptedException, ExecutionException {
        PullRequestStatus status = service.getStatus(pr).get();
        assertNotNull(status.getTransaction());
        return status;
    }

    public @Test void testCheckMergeability() throws InterruptedException, ExecutionException {
        PullRequest pr = service.create(request);
        CompletableFuture<PullRequestStatus> future = service.checkMergeable(pr, dave.getId());
        assertNotNull(future);
        PullRequestStatus status = future.get();
        assertNotNull(status);
        System.err.println(status);
    }

    public @Test void testCheckMergeabilityConcurrency()
            throws InterruptedException, ExecutionException {
        PullRequest pr = service.create(request);

        await().atMost(1, SECONDS).until(() -> !listener.prepareStart.isEmpty());
        assertEquals(1, listener.prepareStart.size());

        await().atMost(1, SECONDS).until(() -> !listener.prepareEnd.isEmpty());
        listener.clear();

        CompletableFuture<PullRequestStatus> f1 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f2 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f3 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f4 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f5 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f6 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f7 = service.checkMergeable(pr, dave.getId());
        CompletableFuture<PullRequestStatus> f8 = service.checkMergeable(pr, dave.getId());

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3, f4, f5, f6, f7, f8);
        all.join();
        assertEquals(1, listener.prepareStart.size());

        listener.clear();

        service.checkMergeable(pr, dave.getId()).get();
        service.checkMergeable(pr, dave.getId()).get();
        service.checkMergeable(pr, dave.getId()).get();
        assertEquals(3, listener.prepareStart.size());
    }

    public @Test void testMerge() throws ExecutionException {
        PullRequest pr = service.create(request);
        PullRequestStatus status = merge(pr);
        assertNotNull(status);

        User issuer = support.getUsers().requireAuthenticatedUser();
        RepoInfo issuerRepoInfo = support.getRepositoryInfo(pr.getIssuerRepo());

        ObjectId mergeCommitId = ObjectId.valueOf(status.getMergeCommitId().get());

        Repository repository = support.getRepos().resolve(pr.getRepositoryId());
        Optional<RevCommit> mc = repository.command(ResolveCommit.class)
                .setCommitIsh(pr.getTargetBranch()).call();
        assertTrue(mc.isPresent());
        assertEquals(mergeCommitId, mc.get().getId());
        RevCommit mergeCommit = mc.get();
        assertEquals(2, mergeCommit.getParentIds().size());
    }

    public @Test void testMergeCommitMessage() throws ExecutionException {
        PullRequest pr = service.create(request);
        PullRequestStatus status = merge(pr);
        assertNotNull(status);

        User issuer = support.getUsers().requireAuthenticatedUser();
        RepoInfo issuerRepoInfo = support.getRepositoryInfo(pr.getIssuerRepo());

        ObjectId mergeCommitId = ObjectId.valueOf(status.getMergeCommitId().get());
        String expectedMessage = String.format("Merge pull request #%d from %s:%s/%s", pr.getId(),
                issuer.getIdentity(), issuerRepoInfo.getIdentity(), pr.getIssuerBranch());

        Repository repository = support.getRepos().resolve(pr.getRepositoryId());
        RevCommit mc = repository.objectDatabase().getCommit(mergeCommitId);
        assertEquals(expectedMessage, mc.getMessage());
    }

    public @Test void testMergeFailsOnConflicts() throws ExecutionException {
        createConflicts(TestData.point1, TestData.line1, TestData.poly1);

        PullRequest pr = service.create(request);

        mergeExpectConflicts(pr, 3);

        PullRequestStatus status = service.getStatus(pr).join();
        assertEquals(MergeableStatus.UNMERGEABLE, status.getMergeable());
        assertNotNull(status.getTransaction());

    }

    public @Test void testFixConflictsThenMerge() throws Exception {
        createConflicts(TestData.point1, TestData.line1, TestData.poly1);

        PullRequest pr = service.create(request);

        mergeExpectConflicts(pr, 3);
        PullRequestStatus status = service.getStatus(pr).join();
        assertEquals(MergeableStatus.UNMERGEABLE, status.getMergeable());
        assertNotNull(status.getTransaction());

        fixConflict(TestData.point1);
        mergeExpectConflicts(pr, 2);

        fixConflict(TestData.line1);
        mergeExpectConflicts(pr, 1);

        fixConflict(TestData.poly1);

        PullRequestStatus result = merge(pr);
        assertEquals(MergeableStatus.MERGED, result.getMergeable());
    }

    public @Test void testClose() {
        PullRequest created = service.create(request);
        assertNotNull(created);

        PullRequest closed = service.close(dave.getId(), gabe.getIdentity(),
                originRepoInfo.getIdentity(), created.getId());
        assertEquals(PullRequest.Status.CLOSED, closed.getStatus());
        assertNotNull(closed.getUpdatedAt());
        assertEquals(dave.getId(), closed.getClosedByUserId());
    }

    public @Test void testDelMod() throws Exception {
        final RepoInfo qaqcRepoInfo = support.runAs("gabe").createRepo("QAQC");
        TestData data_qaqc = new TestData(support.getRepository(qaqcRepoInfo));
        data_qaqc.loadDefaultData();

        final RepoInfo modifyRepoInfo = support.runAs("dave").forkAs("gabe", "QAQC", "modifyRepo");
        final RepoInfo delRepoInfo = support.runAs("dave").forkAs("gabe", "QAQC", "delRepo");

        {
            TestData data_modify = new TestData(support.getRepository(modifyRepoInfo));
            Map<String, SimpleFeature> fs = data_modify.getFeatures("master");
            SimpleFeature f = fs.get("Points/Point.1");
            f.setAttribute("sp", "modified");
            String m = String.format("change %s on modifyRepo", f.getID());
            data_modify.checkout("master").insert(f).add().commit(m);
            // verify
            ArrayList<RevCommit> log = newArrayList(data_modify.log("master"));
            assertEquals(m, log.get(0).getMessage());
            fs = data_modify.getFeatures("master");
            f = fs.get("Points/Point.1");
            assertEquals("modified", f.getAttribute("sp"));
        }
        {
            TestData data_del = new TestData(support.getRepository(delRepoInfo));
            Map<String, SimpleFeature> fs2 = data_del.getFeatures("master");
            SimpleFeature f = fs2.get("Points/Point.1");
            f.setAttribute("sp", "modified2");
            String m = String.format("remove %s on delRepo", f.getID());
            data_del.checkout("master").remove(f).add().commit(m);
            ArrayList<RevCommit> log2 = newArrayList(data_del.log("master"));
            assertEquals(m, log2.get(0).getMessage());
            fs2 = data_del.getFeatures("master");
            f = fs2.getOrDefault("Points/Point.1", null);
            assertNull(f);
        }
        // setup done, build PRs
        PullRequestRequest prModify = PullRequestRequest.builder()//
                .targetRepo(qaqcRepoInfo.getId())//
                .targetBranch("master")//
                .issuerUser(dave.getId())//
                .issuerRepo(modifyRepoInfo.getId())//
                .issuerBranch("master")//
                .title("My PR - modify")//
                .description("PR description - modify")//
                .build();
        PullRequest pr_mod = service.create(prModify);
        PullRequestRequest prDelete = PullRequestRequest.builder()//
                .targetRepo(qaqcRepoInfo.getId())//
                .targetBranch("master")//
                .issuerUser(dave.getId())//
                .issuerRepo(delRepoInfo.getId())//
                .issuerBranch("master")//
                .title("My PR - del")//
                .description("PR description - del")//
                .build();
        PullRequest pr_del = service.create(prDelete);
        PullRequestStatus status = merge(pr_mod);
        Map<String, SimpleFeature> fs3 = data_qaqc.getFeatures("master");
        SimpleFeature f = fs3.get("Points/Point.1");
        assertEquals("modified", f.getAttribute("sp"));
        ArrayList<RevCommit> log3 = newArrayList(data_qaqc.log("master"));
        // THIS SHOULD FAIL
        mergeExpectConflicts(pr_del, 1);
        // PullRequestStatus status2 = merge(pr_del);
        // ArrayList<RevCommit> log4 = newArrayList(data_qaqc.log("master"));
        // Map<String, SimpleFeature> fs4 = data_qaqc.getFeatures("master");
    }

    // resolves a conflict on the issuer branch without synchronizing, by just setting the feature
    // attribute value to the same value than in the target repo/branch and committing
    private void fixConflict(SimpleFeature feature) throws Exception {

        String layerName = feature.getType().getTypeName();

        FeatureFilter filter = new FeatureFilter().addFeatureIdsItem(feature.getID());
        GeogigFeature featureInTargetRepo = featureService
                .getFeatures("gabe", originRepoInfo.getIdentity(), layerName,
                        new FeatureQuery().head("master").filter(filter), null)
                .next();

        Transaction transaction = transactions.beginTransaction(forkRepoInfo.getId());
        UUID txId = transaction.getId();
        String user = dave.getIdentity();
        String repo = forkRepoInfo.getIdentity();

        SimpleFeature featureFix = TestData.clone(feature);
        featureFix.setAttribute("sp", featureInTargetRepo.get("sp"));

        branches.checkout(user, repo, "issuerBranch", txId, true);
        LayerInfo layer = featureService.getLayer(user, repo, layerName, "issuerBranch", txId);
        RevisionFeatureType type = layer.getType();
        GeogigFeatureCollection collection = GeogigFeatureCollection.of(type,
                GeoToolsDomainBridge.toFeature(featureFix));

        featureService.insert(user, repo, layerName, collection, txId);

        GeogigFeature next = featureService.getFeatures(user, repo, layerName,
                new FeatureQuery().head("WORK_HEAD").filter(filter), txId).next();
        assertEquals(featureInTargetRepo.get("sp"), next.get("sp"));

        String commitMessage = String.format("Fix conflict for %s/%s", layerName,
                featureFix.getID());
        CompletableFuture<Transaction> future = transactions.commit(dave, transaction,
                commitMessage);
        future.join();

        Map<String, SimpleFeature> features = clone.getFeatures("issuerBranch", layerName);
        SimpleFeature f = features.get(String.format("%s/%s", layerName, featureFix.getID()));
        assertEquals(featureInTargetRepo.get("sp"), f.getAttribute("sp"));

        ObjectId commitId = clone.getRef("issuerBranch").getObjectId();
        RevCommit commit = clone.getContext().objectDatabase().getCommit(commitId);
        assertEquals(commitMessage, commit.getMessage());
    }

    private PullRequestStatus merge(PullRequest pr) {
        UUID prRepoId = pr.getRepositoryId();
        RepoInfo targetRepo = support.getRepositoryInfo(prRepoId);
        User targetRepoOwner = support.getUser(targetRepo.getOwnerId());
        String user = targetRepoOwner.getIdentity();
        String repo = targetRepo.getIdentity();

        Task<PullRequestStatus> task = service.merge(user, repo, pr.getId(), "title", "message");

        PullRequestStatus status;
        try {
            status = task.getFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        PullRequest postMerge = service.getOrFail(pr.getRepositoryId(), pr.getId());
        assertEquals(PullRequest.Status.MERGED, postMerge.getStatus());
        assertNotNull(postMerge.getUpdatedAt());
        assertNotNull(postMerge.getClosedByUserId());
        return status;
    }

    private void mergeExpectConflicts(PullRequest pr, int ccount) {
        UUID prRepoId = pr.getRepositoryId();
        RepoInfo targetRepo = support.getRepositoryInfo(prRepoId);
        User targetRepoOwner = support.getUser(targetRepo.getOwnerId());
        String user = targetRepoOwner.getIdentity();
        String repo = targetRepo.getIdentity();

        Task<PullRequestStatus> task = service.merge(user, repo, pr.getId(), "title", "message");

        PullRequestStatus status;
        try {
            status = task.getFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        assertEquals(ccount, status.getNumConflicts());
    }

    private void createConflicts(SimpleFeature... features) {
        for (SimpleFeature base : features) {
            SimpleFeature toOrigin = TestData.clone(base);
            toOrigin.setAttribute("sp", "modified on origin");

            origin.checkout("master").insert(toOrigin).add()
                    .commit(String.format("change %s on origin", base.getID()));

            SimpleFeature toClone = TestData.clone(base);
            toClone.setAttribute("sp", "modified on fork");

            clone.checkout("issuerBranch").insert(toClone).add()
                    .commit(String.format("change %s on clone", base.getID()));
        }
    }
}
