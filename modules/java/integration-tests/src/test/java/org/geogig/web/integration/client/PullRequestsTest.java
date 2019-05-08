package org.geogig.web.integration.client;

import static org.awaitility.Awaitility.await;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.CHECKING;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.MERGEABLE;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.MERGED;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.MERGING;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.UNKNOWN;
import static org.geogig.web.model.PullRequestStatus.MergeableEnum.UNMERGEABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.web.client.AsyncTask;
import org.geogig.web.client.Branch;
import org.geogig.web.client.Layer;
import org.geogig.web.client.PreconditionRequiredException;
import org.geogig.web.client.PullRequest;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.PullRequestStatus.MergeableEnum;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionFeatureType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

public class PullRequestsTest extends AbstractIntegrationTest {

    private Store defaultStore, secondStore;

    private User user1, user2;

    private Repo origin, fork;

    private Layer poiLayer;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        testSupport.setDebugging(false);
        String storeName = testName.getMethodName() + "_store";
        defaultStore = testSupport.createStore(storeName, "Default store for all");
        user1 = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);

        user2 = testSupport.createUser("dave", "s3cr3t", "David Blasby", "dave@example.com",
                storeName);

        origin = user1.createRepo("naturalEearth");
        poiLayer = createPoiLayer(origin);

        AsyncTask<RepositoryInfo> forkTask = user2.repositories().fork(origin);
        assertTrue(forkTask.awaitTermination().isComplete());
        fork = user2.getRepo(origin.getIdentity());
    }

    private void assertMergeable(PullRequest pr, MergeableEnum... expected)
            throws TimeoutException {
        PullRequestStatus status = pr.status();
        EnumSet<MergeableEnum> exp = EnumSet.of(expected[0], expected);
        try {
            for (int i = 0; i < 5 && !exp.contains(status.getMergeable()); i++) {
                Thread.sleep(50);
                status = pr.status();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(String.format("expected one of %s, got %s", exp, status.getMergeable()),
                exp.contains(status.getMergeable()));
    }

    // a PR cannot be issued to the same branch
    public @Test(expected = IllegalArgumentException.class) void tryCreateSameRepoSameBanch() {
        Branch master = origin.branches().getCurrentBranch();
        Branch master2 = origin.branches().getCurrentBranch();
        master2.pullRequestTo(master, "title", "description");
    }

    // a PR shall be created from the same repo, a fork, or a fork of a fork
    public @Test(expected = IllegalArgumentException.class) void tryCreateFromNonForkRepo() {
        Repo unrelatedRepo = user2.createRepo("unrelatedRepo");
        createPoiLayer(unrelatedRepo);

        Branch master = origin.branches().getCurrentBranch();
        Branch unrelatedBranch = unrelatedRepo.branches().getCurrentBranch();
        unrelatedBranch.pullRequestTo(master, "title", "description");
    }

    public @Test void createPRWithNoChanges() throws Exception {
        Branch master = origin.branches().getCurrentBranch();
        Branch branch = master.branch("temporaryBranch");

        PullRequest pr = branch.pullRequestTo(master, "My first PR", null);
        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);
        RevisionCommit masterCommit = master.getCommit();
        RevisionCommit sourceCommit = pr.sourceCommit();
        assertEquals(masterCommit, sourceCommit);

        PullRequestStatus status = pr.status();
        assertTrue(status.getAffectedLayers().isEmpty());

        for (int i = 0; i < 10 && !MERGEABLE.equals(status.getMergeable()); i++) {
            Thread.sleep(10);
            status = pr.status();
        }
        assertMergeable(pr, MERGEABLE);
    }

    public @Test void createPRFastForwardChangesSameRepo() throws TimeoutException {
        // the target repo hasn't changed
        Branch master = origin.branches().getCurrentBranch();

        final RevisionCommit originCommit = master.getCommit();

        final String branchName = "temporaryBranch";

        origin.startTransaction();
        Branch branch = master.branch(branchName).checkout();
        // a simple change on the fork, a new layer
        super.createRoadsLayer(origin);
        origin.commit("Created roads layer").awaitTermination();

        final RevisionCommit prCommit = branch.refresh().getCommit();
        assertNotEquals(originCommit, prCommit);
        assertEquals(originCommit, master.refresh().getCommit());

        PullRequest pr = branch.pullRequestTo(master, "My first PR", null);
        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);

        RevisionCommit sourceCommit = pr.sourceCommit();
        assertNotEquals(originCommit, sourceCommit);
        assertEquals(prCommit, sourceCommit);

        assertMergeable(pr, MERGEABLE);
    }

    public @Test void createFastForwardForkRepo() throws TimeoutException {
        // a simple change on the fork, a new layer
        super.createRoadsLayer(fork);

        // the target repo hasn't changed
        Branch master = origin.branches().getCurrentBranch();

        Branch branch = master.branch("temporaryBranch");

        PullRequest pr = branch.pullRequestTo(master, "My first PR", null);
        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);

        RevisionCommit masterCommit = master.getCommit();
        RevisionCommit sourceCommit = pr.sourceCommit();
        assertEquals(masterCommit, sourceCommit);

        Optional<PullRequest> retrieved = master.getRepo().pullRequests().get(pr.getId());
        assertTrue(retrieved.isPresent());

        assertMergeable(pr, MERGEABLE);
    }

    public @Test void createFastForwardForkOfFork() throws TimeoutException {

        assertTrue(user2.repositories().fork(fork, "forkOfFork").awaitTermination().isComplete());
        Repo forkOfFork = user2.getRepo("forkOfFork");

        // a simple change on the fork, a new layer
        super.createRoadsLayer(forkOfFork);

        // the target repo hasn't changed
        Branch target = origin.branches().getCurrentBranch();

        Branch from = forkOfFork.branches().getCurrentBranch();

        PullRequest pr = from.pullRequestTo(target, "Pr fork of fork to upstream", null);
        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);

        RevisionCommit masterCommit = from.getCommit();
        RevisionCommit sourceCommit = pr.sourceCommit();
        assertEquals(masterCommit, sourceCommit);

        Optional<PullRequest> retrieved = target.getRepo().pullRequests().get(pr.getId());
        assertTrue(retrieved.isPresent());

        assertMergeable(pr, MERGEABLE);
    }

    public @Test void getPrCommitsSameRepo() throws TimeoutException {
        // the target repo hasn't changed
        final Repo origin = this.origin;
        final Repo issuerRepo = this.origin;
        testGetPrCommits(origin, issuerRepo);
    }

    public @Test void getPrCommitsDifferentRepo() throws TimeoutException {
        // the target repo hasn't changed
        final Repo origin = this.origin;
        final Repo issuerRepo = this.fork;
        testGetPrCommits(origin, issuerRepo);
    }

    private void testGetPrCommits(final Repo origin, final Repo issuerRepo)
            throws TimeoutException {
        final Branch originMaster = origin.branches().getCurrentBranch();
        final RevisionCommit commonAncestor = originMaster.getCommit();

        Branch featureBranch = issuerRepo.branches().get(originMaster.getName())
                .branch("featureBranch");

        GeogigFeature[] poiFeatures = testSupport.poiFeatures();
        List<RevisionCommit> commits = new ArrayList<>();
        final RevisionFeatureType type = testSupport.poiFeatureType();
        int nfeatures = 0;
        for (GeogigFeature f : poiFeatures) {
            issuerRepo.startTransaction();
            featureBranch.checkout();
            featureBranch.featureService().addFeatures(GeogigFeatureCollection.of(type, f));
            issuerRepo.commit("insert poi feature " + f.getId()).awaitTermination().getResult();
            RevisionCommit commit = featureBranch.refresh().getCommit();
            commits.add(0, commit);
            nfeatures++;
            long size = featureBranch.featureService().getLayer(type.getName()).getSize();
            assertEquals(nfeatures, size);
        }

        String title = "Poi Features PR";
        String description = String.format("%d commits, one per feature", poiFeatures.length);
        PullRequest pr = featureBranch.pullRequestTo(originMaster, title, description);
        assertMergeable(pr, MERGEABLE);

        List<RevisionCommit> prCommits = pr.getCommits();
        assertNotNull(prCommits);
        assertEquals(poiFeatures.length, prCommits.size());
        assertEquals(commits, prCommits);
    }

    public @Test void getDiffSummarySameRepo() throws TimeoutException {
        // the target repo hasn't changed
        final Repo origin = this.origin;
        final Repo issuerRepo = this.origin;
        testGetPrDiffSummary(origin, issuerRepo);
    }

    public @Test void getDiffSummaryDifferentRepo() throws TimeoutException {
        // the target repo hasn't changed
        final Repo origin = this.origin;
        final Repo issuerRepo = this.fork;
        testGetPrDiffSummary(origin, issuerRepo);
    }

    public @Test void prMergeSameRepoFastFowrward() throws TimeoutException {
        final RevisionFeatureType type = testSupport.poiFeatureType();
        final String layer = type.getName();
        final Branch master = origin.branches().getCurrentBranch();
        final Branch branch = master.branch("temporaryBranch");
        final GeogigFeature[] features = testSupport.poiFeatures();

        assertEquals(0, master.featureService().getLayer(layer).getSize());
        assertEquals(0, branch.featureService().getLayer(layer).getSize());

        PullRequest pr = testSupport.worker(origin)//
                .startTransaction()//
                .checkout(branch.getName())//
                .insert(layer, features)//
                .commitTransaction("add poi features")//
                .pullRequest(branch.getName(), origin, master.getName())//
                .get();

        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);

        assertEquals(0, master.featureService().getLayer(layer).getSize());
        assertEquals(features.length, branch.featureService().getLayer(layer).getSize());
        assertTrue(origin.pullRequests().get(pr.getId()).isPresent());

        assertMergeable(pr, MERGEABLE);

        AsyncTask<PullRequestStatus> asyncTask = pr.merge().awaitTermination();
        assertTrue(asyncTask.isComplete());
        PullRequestStatus result = asyncTask.getResult();
        assertNotNull(result);
        assertEquals(MERGED, result.getMergeable());
        assertEquals(features.length, master.featureService().getLayer(layer).getSize());
        assertEquals(Sets.newHashSet(layer), Sets.newHashSet(result.getAffectedLayers()));
    }

    public @Test void prMergeSameRepoNoFastFowrward() throws TimeoutException {
        final RevisionFeatureType type = testSupport.poiFeatureType();
        final String layer = type.getName();
        final Branch master = origin.branches().getCurrentBranch();
        final Branch branch = master.branch("temporaryBranch");

        PullRequest pr = testSupport.worker(origin)//
                .startTransaction()//
                .checkout(master.getName())//
                .insert(layer, poi(0), poi(1))//
                .commitTransaction("changes on master")//
                .startTransaction()//
                .checkout(branch.getName())//
                .insert(layer, poi(2), poi(3), poi(4))//
                .commitTransaction("changes on branch")//
                .pullRequest(branch.getName(), origin, master.getName())//
                .get();

        assertNotNull(pr);
        assertMergeable(pr, UNKNOWN, CHECKING);

        assertEquals(2, master.featureService().getLayer(layer).getSize());
        assertEquals(3, branch.featureService().getLayer(layer).getSize());
        assertTrue(origin.pullRequests().get(pr.getId()).isPresent());

        assertMergeable(pr, MERGEABLE);

        AsyncTask<PullRequestStatus> asyncTask = pr.merge().awaitTermination();
        assertTrue(asyncTask.isComplete());
        PullRequestStatus result = asyncTask.getResult();
        assertNotNull(result);
        assertEquals(MERGED, result.getMergeable());
        assertEquals(5, master.featureService().getLayer(layer).getSize());
    }

    public @Test void prMergeDifferentRepoNoFF() throws Exception {
        final String layer = testSupport.poiFeatureType().getName();
        PullRequest pr = testSupport.worker(fork)//
                .branch("master", "whatif")//
                .startTransaction()//
                .checkout("whatif")//
                .insert(layer, poi(0), poi(1), poi(2), poi(3))//
                .commitTransaction("add poi [0,1,2,3] fork")//
                .pullRequest("whatif", origin, "master").get();

        testSupport.worker(origin)//
                .startTransaction()//
                .checkout("master")//
                .insert(layer, poi(0), poi(1))//
                .commitTransaction("add poi [0,1] origin");

        Optional<PullRequest> prop = origin.pullRequests().get(pr.getId());
        assertTrue(prop.isPresent());
        assertEquals(pr.getInfo().getId(), prop.get().getInfo().getId());

        RevisionCommit oursCommit = origin.branches().get("master").getCommit();
        RevisionCommit theirsCommit = fork.branches().get("whatif").getCommit();
        RevisionCommit commonAncestor = fork.branches().get("master").getCommit();
        assertNotEquals(oursCommit, theirsCommit);
        assertNotEquals(theirsCommit, commonAncestor);

        assertEquals(2, origin.branches().get("master").featureService().getLayer(layer).getSize());
        assertEquals(0, fork.branches().get("master").featureService().getLayer(layer).getSize());
        assertEquals(4, fork.branches().get("whatif").featureService().getLayer(layer).getSize());

        AsyncTask<PullRequestStatus> mergeTask = pr.merge().awaitTermination();
        assertTrue(mergeTask.isComplete());
        PullRequestStatus result = mergeTask.getResult();
        assertNotNull(result);
        assertEquals(MERGED, result.getMergeable());

        // RevisionCommit mergeCommit = result.getMergeCommit();
        // assertNotNull(mergeCommit);
        // assertEquals(commonAncestor, result.getCommonAncestor());
        // assertEquals(oursCommit, result.getOursCommit());
        // assertEquals(theirsCommit, result.getTheirsCommit());

        assertEquals(4, origin.branches().get("master").featureService().getLayer(layer).getSize());
    }

    public @Test void mergeDifferentRepoConflicts() throws Exception {
        final String layer = testSupport.poiFeatureType().getName();

        GeogigFeature conflictFeature = poi(0);

        final Branch originMaster = testSupport.worker(origin)//
                .startTransaction()//
                .insert(layer, conflictFeature, poi(1))//
                .commitTransaction("add poi [0,1] origin")//
                .getBranch("master");

        assertEquals("Feature 0", conflictFeature.get("name"));
        conflictFeature.put("name", "Conflicting name");

        final Branch forkBranch = testSupport.worker(fork)//
                .branch("master", "whatif")//
                .startTransaction()//
                .checkout("whatif")//
                .insert(layer, conflictFeature, poi(1), poi(2), poi(3))//
                .commitTransaction("add poi [0,1,2,3] fork")//
                .getBranch("whatif");

        assertEquals(2, originMaster.featureService().getLayer(layer).getSize());
        assertEquals(4, forkBranch.featureService().getLayer(layer).getSize());

        PullRequest pr = testSupport.worker(fork)//
                .pullRequest("whatif", origin, "master")//
                .get();

        AsyncTask<PullRequestStatus> mergeTask = pr.merge().awaitTermination();
        assertTrue(mergeTask.isFinished());
        assertTrue("The async task should have completed normally", mergeTask.isComplete());
        assertEquals(StatusEnum.COMPLETE, mergeTask.getStatus());
        PullRequestStatus result = mergeTask.getResult();
        assertEquals("The pr status should be UNMERGEABLE", MergeableEnum.UNMERGEABLE,
                result.getMergeable());
        assertEquals("Expected one conflict", 1, result.getNumConflicts().intValue());

        // make sure nothing has changed
        assertEquals(2, originMaster.featureService().getLayer(layer).getSize());
        assertEquals(4, forkBranch.featureService().getLayer(layer).getSize());

        // fix conflicts
        conflictFeature.put("name", "Feature 0");
        testSupport.worker(fork)//
                .startTransaction()//
                .checkout("whatif")//
                .insert(layer, conflictFeature)//
                .commitTransaction("Fix conflicts");

        pr = origin.pullRequests().get(pr.getId()).get();
        mergeTask = pr.merge();
        PullRequestStatus status = pr.status();
        assertEquals(Sets.newHashSet(layer), Sets.newHashSet(status.getAffectedLayers()));

        for (int i = 0; i < 10 && UNMERGEABLE.equals(status.getMergeable()); i++) {
            Thread.sleep(10);
            status = pr.status();
        }
        assertTrue(status.toString(),
                MERGING.equals(status.getMergeable()) || MERGED.equals(status.getMergeable()));
        assertEquals(Sets.newHashSet(layer), Sets.newHashSet(status.getAffectedLayers()));

        mergeTask.awaitTermination();

        assertTrue(mergeTask.isComplete());
        result = mergeTask.getResult();
        assertEquals(MERGED, result.getMergeable());

        assertEquals(4, originMaster.featureService().getLayer(layer).getSize());
        pr = pr.refresh();
        PullRequestInfo info = pr.getInfo();
        assertEquals(org.geogig.web.model.PullRequestInfo.StatusEnum.MERGED, info.getStatus());
        assertNotNull(info.getClosedAt());
        assertNotNull(info.getClosedBy());
    }

    @Ignore // waiting to implement generic pull to resolve conflicts on the issuer branch
    public @Test void mergeDifferentRepoConflictsResolveSyncingIssuerRepo() throws Exception {
        final String layer = testSupport.poiFeatureType().getName();

        final GeogigFeature base = poi(0);
        GeogigFeature conflictFeature = poi(0);

        final Branch originMaster = testSupport.worker(origin)//
                .startTransaction()//
                .insert(layer, conflictFeature, poi(1))//
                .commitTransaction("add poi [0,1] origin")//
                .getBranch("master");

        assertEquals("Feature 0", conflictFeature.get("name"));
        conflictFeature.put("name", "Conflicting name");

        final Branch forkBranch = testSupport.worker(fork)//
                .branch("master", "whatif")//
                .startTransaction()//
                .checkout("whatif")//
                .insert(layer, conflictFeature, poi(1), poi(2), poi(3))//
                .commitTransaction("add poi [0,1,2,3] fork")//
                .getBranch("whatif");

        assertEquals(2, originMaster.featureService().getLayer(layer).getSize());
        assertEquals(4, forkBranch.featureService().getLayer(layer).getSize());

        PullRequest pr = testSupport.worker(fork)//
                .pullRequest("whatif", origin, "master")//
                .get();

        AsyncTask<PullRequestStatus> mergeTask = pr.merge().awaitTermination();

        // make sure nothing has changed
        assertEquals(2, originMaster.featureService().getLayer(layer).getSize());
        assertEquals(4, forkBranch.featureService().getLayer(layer).getSize());

        // fix conflicts by staging, effectively using an "ours" strategy (i.e. mark what's already
        // in the working tree as valid)
        PullRequestStatus status = pr.status();
        assertEquals(1L, status.getNumConflicts().longValue());

        Repo transaction = forkBranch.getRepo().startTransaction();
        AsyncTask<MergeResult> asyncTask = forkBranch.sycWithTrackedBranch().awaitTermination();

        UUID transactionId = status.getTransaction();
        String path = String.format("%s/%s", layer, conflictFeature.getId());
        origin.resume(transactionId).stage(path).awaitTermination();
        origin.exitTransaction();

        status = pr.status();
        assertEquals(0L, status.getNumConflicts().longValue());

        mergeTask = pr.merge().awaitTermination();
        assertTrue(mergeTask.isComplete());
        PullRequestStatus result = mergeTask.getResult();
        assertEquals(MERGED, result.getMergeable());

        assertEquals(4, originMaster.featureService().getLayer(layer).getSize());
        pr = pr.refresh();
        PullRequestInfo info = pr.getInfo();
        assertEquals(org.geogig.web.model.PullRequestInfo.StatusEnum.MERGED, info.getStatus());
        assertNotNull(info.getClosedAt());
        assertNotNull(info.getClosedBy());
    }

    /**
     * Call {@code /repos/{user}/{repo}/pulls/{pr}/diff/summary} through
     * {@link PullRequest#getDiffSummary()} to get a list of {@link LayerDiffSummary}, one per layer
     * changed by the PR.
     */
    private void testGetPrDiffSummary(final Repo origin, final Repo issuerRepo)
            throws TimeoutException {

        final Branch originMaster = origin.branches().getCurrentBranch();
        final Branch featureBranch = issuerRepo.branches().get(originMaster.getName())
                .branch("featureBranch");

        GeogigFeature[] poiFeatures = testSupport.poiFeatures();
        List<RevisionCommit> commits = new ArrayList<>();
        final RevisionFeatureType type = testSupport.poiFeatureType();
        int nfeatures = 0;
        for (GeogigFeature f : poiFeatures) {
            issuerRepo.startTransaction();
            featureBranch.checkout();
            featureBranch.featureService().addFeatures(GeogigFeatureCollection.of(type, f));
            issuerRepo.commit("insert poi feature " + f.getId()).awaitTermination().getResult();
            RevisionCommit commit = featureBranch.refresh().getCommit();
            commits.add(0, commit);
            nfeatures++;
            long size = featureBranch.featureService().getLayer(type.getName()).getSize();
            assertEquals(nfeatures, size);
        }

        String title = "Poi Features PR";
        String description = String.format("%d commits, one per feature", poiFeatures.length);
        PullRequest pr = featureBranch.pullRequestTo(originMaster, title, description);
        assertMergeable(pr, MERGEABLE);

        List<LayerDiffSummary> summary = pr.getDiffSummary();
        assertNotNull(summary);
        assertEquals(1, summary.size());
        LayerDiffSummary ld = summary.get(0);
        assertEquals(type.getName(), ld.getPath());
        assertEquals(poiFeatures.length, ld.getFeaturesAdded().intValue());

        assertNull(ld.getLeftBounds());
        assertNotNull(ld.getRightBounds());
        assertFalse(GeogigObjectModelBridge.toBounds(ld.getRightBounds()).isNull());
    }

    public @Ignore @Test void updatePRTitleAndDescription() {
        fail("not yet implemented");
    }

    public @Test void getPullRequestDiffFeaturesConflicts() throws IOException {
        final GeogigFeature[] features = testSupport.poiFeatures();
        GeogigFeature poi1Modified = new GeogigFeature(features[1]);
        poi1Modified.put("name", "changed name of Feature 1");

        //@formatter:off
        // repo1 master: features[0], features[1] , null       , features[3]
        // repo2 master: null       , poi1Modified, features[2], features[3]
        //@formatter:on

        testSupport.worker(origin)//
                .startTransaction()//
                .insert(poiLayer.getName(), features[0], features[1], features[3])//
                .commitTransaction("origin commit");

        final PullRequest pullRequest = testSupport.worker(fork)//
                .startTransaction()//
                .insert(poiLayer.getName(), poi1Modified, features[2], features[3])//
                .commitTransaction("second repo commit")//
                .pullRequest("master", origin, "master").get();

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> MergeableEnum.UNMERGEABLE.equals(pullRequest.status().getMergeable()));

        GeogigFeatureCollection conflicts = pullRequest.getConflictFeatures(poiLayer.getName());
        assertEquals(1, Iterators.size(conflicts));

        ex.expect(PreconditionRequiredException.class);
        ex.expectMessage("has merge conflicts, can't compute diff features");
        pullRequest.getDiffFeatures(poiLayer.getName());
    }

    public @Test void getPullRequestDiffFeaturesMergeable() throws IOException {
        final GeogigFeature[] features = testSupport.poiFeatures();
        GeogigFeature poi1Modified = new GeogigFeature(features[1]);
        poi1Modified.put("name", "changed name of Feature 1");

        //@formatter:off
        // repo1 master: features[0], features[1] , null      , null
        // repo2 master: null       , features[1], features[2], features[3]
        //@formatter:on

        testSupport.worker(origin)//
                .startTransaction()//
                .insert(poiLayer.getName(), features[0], features[1])//
                .commitTransaction("origin commit");

        final PullRequest pullRequest = testSupport.worker(fork)//
                .startTransaction()//
                .insert(poiLayer.getName(), features[1], features[2], features[3])//
                .commitTransaction("second repo commit")//
                .pullRequest("master", origin, "master").get();

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> MergeableEnum.MERGEABLE.equals(pullRequest.status().getMergeable()));

        GeogigFeatureCollection collection = pullRequest.getDiffFeatures(poiLayer.getName());
        assertTrue(collection.getSize().isPresent());
        assertTrue(collection.getBounds().isPresent());
        assertEquals(2, collection.getSize().get().intValue());

        Map<String, GeogigFeature> byId = testSupport.toMap(collection);
        testSupport.assertDiffFeature(byId.get(features[2].getId()), null, features[2]);
        testSupport.assertDiffFeature(byId.get(features[3].getId()), null, features[3]);

        ex.expect(PreconditionRequiredException.class);
        ex.expectMessage("has no merge conflicts");
        pullRequest.getConflictFeatures(poiLayer.getName());
    }
}
