package org.geogig.web.integration.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.client.datastore.GeogigWebDatastore;
import org.geogig.web.client.datastore.GeogigWebDiffFeatureSource;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.integration.IntegrationTestSupport;
import org.geogig.web.model.GeogigFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GeogigWebDiffFeatureSourceTest extends AbstractIntegrationTest {

    //@formatter:off
    private static final String USER = "gabe";
    private static final String PASSWORD = "s3cr3t";
    private static final String REPOSITORY = "naturalEearth";
    //@formatter:on

    private Store store;

    private User user;

    private Repo repo;

    /**
     * {@code master} has {@link IntegrationTestSupport#poiFeatures() poi features} 1 and 2, branch has poi
     * features 1, 3, 4.
     */
    private GeogigWebDatastore master;// has poi0, poi1, poi3Modified

    private GeogigWebDatastore branch;// has poi0, poi2, poi3

    public @Rule ExpectedException ex = ExpectedException.none();

    private Layer poi;

    private String layername;

    private GeogigFeature[] features;

    private GeogigFeature poi3Modified;

    public @Before void before() throws IOException {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser(USER, PASSWORD, "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo(REPOSITORY);
        poi = createPoiLayer(repo);
        layername = poi.getName();
        {
            features = testSupport.poiFeatures();
            Branch masterBranch = repo.branches().getCurrentBranch();
            Branch branch = testSupport.worker(repo).branch("master", "branch").get();

            poi3Modified = new GeogigFeature(features[3]);
            poi3Modified.put("name", "name-changed");

            testSupport.worker(repo)//
                    .startTransaction()//
                    .checkout("master")//
                    .insert(layername, features[0], features[1], poi3Modified)//
                    .commitTransaction("master commit");

            testSupport.worker(repo)//
                    .startTransaction()//
                    .checkout("branch")//
                    .insert(layername, features[0], features[2], features[3])//
                    .commitTransaction("branch commit");

            assertEquals(3, masterBranch.featureService().getLayer(layername).getSize());
            assertEquals(3, branch.featureService().getLayer(layername).getSize());
        }
        master = new GeogigWebDatastore(repo.branches().get("master"));

        branch = new GeogigWebDatastore(repo.branches().get("branch"));

        createRoadsLayer(repo);
    }

    public @After void after() {
        assertTrue(user == null || testSupport.deleteUser(user.getIdentity()));
        assertTrue(store == null || testSupport.deleteStore(store.getIdentity()));
    }

    public @Test void getFeaturesNoDiffs() throws IOException {
        GeogigWebDiffFeatureSource diffSource = master.getDiffFeatureSource(layername);
        diffSource.setNewVersion("master");
        diffSource.setOldVersion("master");
        SimpleFeatureType schema = diffSource.getSchema();
        assertNotNull(schema);
        assertDiffSchema(schema);
        SimpleFeatureCollection collection = diffSource.getFeatures();
        assertNotNull(collection);
        SimpleFeatureIterator features = collection.features();
        assertNotNull(features);
        assertFalse(features.hasNext());
    }

    private void assertDiffSchema(SimpleFeatureType schema) {
        assertEquals(3, schema.getAttributeCount());
        assertNotNull(schema.getDescriptor("geogig.changeType"));
        assertNotNull(schema.getDescriptor("old"));
        assertNotNull(schema.getDescriptor("new"));
        assertTrue(schema.getDescriptor("old").getType() instanceof SimpleFeatureType);
        assertTrue(schema.getDescriptor("new").getType() instanceof SimpleFeatureType);
    }

    public @Test void getDiffFeaturesBranch() throws IOException {
        GeogigWebDiffFeatureSource diffSource = branch.getDiffFeatureSource(layername);
        diffSource.setOldVersion(master.getBranch().getName());
        SimpleFeatureCollection collection = diffSource.getFeatures();
        assertEquals(3, collection.size());
        // features[0]: not reported, unchanged, features[1]: removed, features[2]: added,
        // features[3]: modified
        Map<String, SimpleFeature> byId = testSupport.toMap(collection);
        assertEquals(3, byId.size());
        testSupport.assertDiffFeature(byId.get(features[1].getId()), features[1], null);
        testSupport.assertDiffFeature(byId.get(features[2].getId()), null, features[2]);
        testSupport.assertDiffFeature(byId.get(features[3].getId()), poi3Modified, features[3]);
    }

    public @Test void getDiffFeaturesBranchInverse() throws IOException {
        GeogigWebDiffFeatureSource diffSource = master.getDiffFeatureSource(layername);
        diffSource.setOldVersion(branch.getBranch().getName());
        SimpleFeatureCollection collection = diffSource.getFeatures();
        assertEquals(3, collection.size());
        // features[0]: not reported, unchanged, features[1]: added, features[2]: removed,
        // features[3]: modified
        Map<String, SimpleFeature> byId = testSupport.toMap(collection);
        assertEquals(3, byId.size());
        testSupport.assertDiffFeature(byId.get(features[1].getId()), null, features[1]);
        testSupport.assertDiffFeature(byId.get(features[2].getId()), features[2], null);
        testSupport.assertDiffFeature(byId.get(features[3].getId()), features[3], poi3Modified);
    }

    public @Test void getDiffFeaturesCrossRepository() throws IOException {
        GeogigFeature poi1Modified = new GeogigFeature(features[1]);
        poi1Modified.put("name", "changed name of Feature 1");

        // create a second repo with master having the 4 features
        Repo repo2 = user.createRepo("secondrepo");
        testSupport.worker(repo2)//
                .startTransaction()//
                .createLayer(poi.getType())//
                .commitTransaction("create layer")//
                .startTransaction()//
                .insert(poi.getName(), poi1Modified, features[2], features[3])//
                .commitTransaction("second repo commit");

        GeogigWebDatastore repo2DataStore = new GeogigWebDatastore(repo2.branches().get("master"));
        GeogigWebDiffFeatureSource repo2MasterVsRepo1Master;
        GeogigWebDiffFeatureSource repo2MasterVsRepo1Branch;
        {
            String userName = user.getIdentity();
            String repo1Name = repo.getIdentity();

            repo2MasterVsRepo1Master = repo2DataStore.getDiffFeatureSource(poi.getName());
            String repo1RepoMaster = String.format("%s:%s:%s", userName, repo1Name, "master");
            repo2MasterVsRepo1Master.setOldVersion(repo1RepoMaster);

            String repo1RepoBranch = String.format("%s:%s:%s", userName, repo1Name, "branch");
            repo2MasterVsRepo1Branch = repo2DataStore.getDiffFeatureSource(poi.getName());
            repo2MasterVsRepo1Branch.setOldVersion(repo1RepoBranch);
        }
        //@formatter:off
        // repo1 master: features[0], features[1] , null       , poi3Modified
        // repo1 branch: features[0], null        , features[2], features[3]
        // repo2 master: null       , poi1Modified, features[2], features[3]
        //@formatter:on

        SimpleFeatureCollection collection = repo2MasterVsRepo1Master.getFeatures();
        assertEquals(4, collection.size());
        Map<String, SimpleFeature> byId = testSupport.toMap(collection);
        testSupport.assertDiffFeature(byId.get(features[0].getId()), features[0], null);
        testSupport.assertDiffFeature(byId.get(features[1].getId()), features[1], poi1Modified);
        testSupport.assertDiffFeature(byId.get(features[2].getId()), null, features[2]);
        testSupport.assertDiffFeature(byId.get(features[3].getId()), poi3Modified, features[3]);

        collection = repo2MasterVsRepo1Branch.getFeatures();
        assertEquals(2, collection.size());
        byId = testSupport.toMap(collection);
        testSupport.assertDiffFeature(byId.get(features[0].getId()), features[0], null);
        testSupport.assertDiffFeature(byId.get(features[1].getId()), null, poi1Modified);
    }
}
