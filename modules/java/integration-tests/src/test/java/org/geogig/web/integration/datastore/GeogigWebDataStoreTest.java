package org.geogig.web.integration.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.client.datastore.GeogigWebDataStoreFactory;
import org.geogig.web.client.datastore.GeogigWebDatastore;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionFeatureType;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class GeogigWebDataStoreTest extends AbstractIntegrationTest {

    //@formatter:off
    private static final String USER = "gabe";
    private static final String PASSWORD = "s3cr3t";
    private static final String REPOSITORY = "naturalEearth";
    //@formatter:on

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private Store store;

    private User user;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser(USER, PASSWORD, "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo(REPOSITORY);
        createPoiLayer(repo);
    }

    public @Test void testFactoryLookup() throws IOException {
        AtomicBoolean found = new AtomicBoolean(false);
        DataStoreFinder.getAvailableDataStores().forEachRemaining((f) -> {
            if (f instanceof GeogigWebDataStoreFactory) {
                found.set(true);
            }
        });
        assertTrue(found.get());
    }

    public @Test void factoryCreate() throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put(GeogigWebDataStoreFactory.BASE_URL.key, testSupport.getBaseURL());
        params.put(GeogigWebDataStoreFactory.HTTP_USER.key, USER);
        params.put(GeogigWebDataStoreFactory.HTTP_PASSWORD.key, PASSWORD);
        params.put(GeogigWebDataStoreFactory.REPO_OWNER.key, USER);
        params.put(GeogigWebDataStoreFactory.REPO_NAME.key, REPOSITORY);

        DataStore dataStore = DataStoreFinder.getDataStore(params);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeogigWebDatastore);
    }

    public @Test(expected = IOException.class) void factoryCreateBadCredentials()
            throws IOException {
        String badPassword = "badPassword";

        Map<String, Object> params = new HashMap<>();
        params.put(GeogigWebDataStoreFactory.BASE_URL.key, testSupport.getBaseURL());
        params.put(GeogigWebDataStoreFactory.HTTP_USER.key, USER);
        params.put(GeogigWebDataStoreFactory.HTTP_PASSWORD.key, badPassword);
        params.put(GeogigWebDataStoreFactory.REPO_OWNER.key, USER);
        params.put(GeogigWebDataStoreFactory.REPO_NAME.key, REPOSITORY);

        DataStoreFinder.getDataStore(params);
    }

    public @Test(expected = IOException.class) void factoryCreateBadURL() throws IOException {

        String badURL = testSupport.getBaseURL() + "/bad_endpoint";

        Map<String, Object> params = new HashMap<>();
        params.put(GeogigWebDataStoreFactory.BASE_URL.key, badURL);
        params.put(GeogigWebDataStoreFactory.HTTP_USER.key, USER);
        params.put(GeogigWebDataStoreFactory.HTTP_PASSWORD.key, PASSWORD);
        params.put(GeogigWebDataStoreFactory.REPO_OWNER.key, USER);
        params.put(GeogigWebDataStoreFactory.REPO_NAME.key, REPOSITORY);

        DataStoreFinder.getDataStore(params);
    }

    public @Test void createDataStoreDefaultBranch() throws IOException {
        GeogigWebDatastore store;
        Branch branch;

        store = new GeogigWebDatastore(repo);
        branch = store.getBranch();
        assertNotNull(branch);
        assertEquals("master", branch.getName());

        Branch branch2;

        Repo tx = repo.startTransaction();
        branch2 = tx.branches().get("master").branch("newbranch").checkout();
        assertNotNull(branch2);
        assertEquals("newbranch", branch2.getName());

        store = new GeogigWebDatastore(tx);
        branch = store.getBranch();
        assertNotNull(branch);
        assertEquals(branch2.getName(), branch.getName());

        tx.abort();
    }

    public @Test void createDataStoreSpecificBranch() throws IOException {
        GeogigWebDatastore store;

        {
            Repo tx = repo.startTransaction();
            Branch branch2 = tx.branches().get("master").branch("newbranch").checkout();
            assertNotNull(branch2);
            assertEquals("newbranch", branch2.getName());
            tx.commit();
        }
        Branch branch = repo.branches().get("newbranch");
        assertNotNull(branch);
        assertEquals("newbranch", branch.getName());

        store = new GeogigWebDatastore(branch);
        assertNotNull(store.getBranch());
        assertEquals(branch.getName(), store.getBranch().getName());
    }

    public @Test void createSchemaDefaultBranchAutomaticTransaction() throws IOException {
        GeogigWebDatastore store = new GeogigWebDatastore(repo);

        SimpleFeatureType featureType = buildTestFeatureType();

        store.createSchema(featureType);

        SimpleFeatureType retrieved = store.getSchema(featureType.getName());
        assertNotNull(retrieved);
        assertNotSame(featureType, retrieved);
    }

    public @Test void createSchemaNonDefaultBranchAutomaticTransaction() throws IOException {
        final String branchName = "secondBranch";
        {
            Repo tx = repo.startTransaction();
            repo.branches().getCurrentBranch().branch(branchName);
            tx.commit();
        }

        Branch branch = repo.branches().get(branchName);
        assertNotNull(branch);

        GeogigWebDatastore store = new GeogigWebDatastore(branch);
        SimpleFeatureType featureType = buildTestFeatureType();

        store.createSchema(featureType);
        SimpleFeatureType retrieved = store.getSchema(featureType.getName());
        assertNotNull(retrieved);
        assertNotSame(featureType, retrieved);
    }

    public @Test void removeSchema() throws Exception {
        GeogigWebDatastore store = new GeogigWebDatastore(repo);
        RevisionFeatureType poi = testSupport.poiFeatureType();
        assertNotNull(store.getSchema(poi.getName()));

        List<RevisionCommit> logInitial = repo.log();

        store.removeSchema(poi.getName());

        List<RevisionCommit> log = repo.log();
        assertEquals(logInitial.size() + 1, log.size());
        RevisionCommit commit = log.get(0);
        String message = commit.getMessage();
        assertTrue(message.startsWith("Delete FeatureType " + poi.getName()));
        ex.expect(IOException.class);
        store.getSchema(poi.getName());
    }

    private SimpleFeatureType buildTestFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("NewLayer");
        builder.add("name", String.class);
        builder.add("location", Point.class);

        SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
    }
}
