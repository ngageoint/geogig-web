package org.geogig.web.integration;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geogig.web.client.Client;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.client.SimpleTypeBuilder;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.FileStoreInfo;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;
import org.geogig.web.model.UserInfoPublicProfile;
import org.geogig.web.model.UserType;
import org.geogig.web.model.ValueType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * <pre>
 * {@code
 * 1- crear store
 * 2- crear usuario
 * 3- crear repositorio
 * 4- crear feature type
 * 5- agregar un par de features
 * 6- hacer un commit
 * 7- hacer un log
 * }
 * </pre>
 *
 */
@Ignore
public class Example1 {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public TestSupport testSupport = new TestSupport();

    private final String storeName = "Example1 Test Store";

    private final String userName = "groldan";

    private final String userPasswrod = "geogig4all";

    private final String repoName = "testRepo";

    private void log(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }

    public static @BeforeClass void beforeClass() {
        // System.setProperty("geogig.web.client.debug", "true");
    }

    public @Before void before() {
        cleanup();
    }

    public @After void after() {
        cleanup();
    }

    private void cleanup() {
        Client adminClient = testSupport.getAdminClient();
        adminClient.repositories().delete(userName, repoName);
        adminClient.users().delete(userName);
        adminClient.stores().delete(storeName);
    }

    public @Test void testScenario() throws Exception {
        // log-in as admin
        Client adminClient = new Client(testSupport.getBaseURL());
        User admin = adminClient.login(testSupport.getAdminUser(),
                testSupport.getAdminPassword());
        log("logged in as user %s/%s\n", admin.getId(), admin.getIdentity());

        List<Store> stores = adminClient.stores().getAll();
        log("Existing stores: %s\n", Lists.transform(stores, (s) -> s.getIdentity()));

        Store store = createStore(adminClient);
        User newUser = createUser(adminClient, store.getInfo());
        // user is created but can't log in yet, use the admin client to set a password
        newUser.setPassword(userPasswrod);

        // re-fetch the new user with its own credentials
        Client userClient = new Client(testSupport.getBaseURL());
        newUser = userClient.login(userName, userPasswrod);
        // let the new user create a repository
        Repo repo = newUser.createRepo(repoName);

        repo.startTransaction();
        try {
            checkState(repo.branches().getCurrentBranch().featureService().getLayers().isEmpty());
            // create a new Layer
            Layer newLayer = createLayer(repo);
            log("Created layer %s", newLayer.getInfo());

            GeogigFeatureCollection features;
            features = newLayer.getFeatures();
            Assert.assertEquals(0, Iterators.size(features));

            RevisionFeatureType type = newLayer.getType();
            GeogigFeature[] testFeatures = createFeatures(type);
            GeogigFeatureCollection collection = GeogigFeatureCollection.of(type, testFeatures);

            Stopwatch sw = Stopwatch.createStarted();
            newLayer.addFeatures(collection);
            log("Features inserted in %s", sw.stop());

            features = newLayer.getFeatures();
            int size = Iterators.size(features);
            Assert.assertEquals(testFeatures.length, size);

            GeogigFeature original = testFeatures[0];

            List<String> attributes = Arrays.asList("name", "location");
            Geometry newGeom = geom("POINT(0.5 0.5)");
            List<Object> values = Arrays.asList("modified name", newGeom);

            BoundingBox bounds = original.getBounds();
            bounds = expandBy(0.5, 0.5, bounds);
            FeatureFilter filter = new FeatureFilter().bbox(bounds);

            newLayer.modifyFeatures(attributes, values, filter);

            FeatureQuery query = new FeatureQuery().filter(filter);
            try (GeogigFeatureCollection changed = newLayer.getFeatures(query)) {
                GeogigFeature[] changedFeatures = Iterators.toArray(changed, GeogigFeature.class);
                Assert.assertEquals(1, changedFeatures.length);
                GeogigFeature modified = changedFeatures[0];
                Assert.assertEquals("modified name", modified.get("name"));
                Assert.assertEquals(newGeom, modified.get("location"));
            }

            newLayer.removeFeatures(filter);
            try (GeogigFeatureCollection result = newLayer.getFeatures(query)) {
                GeogigFeature[] queryResult = Iterators.toArray(result, GeogigFeature.class);
                Assert.assertEquals(0, queryResult.length);
            }
            TransactionInfo transactionResult = repo.commitSync();
            TransactionStatus status = transactionResult.getStatus();
            assertEquals(TransactionStatus.COMMITTED, status);
            // now check the layer exists in the repository once the transaction has been committed
            assertFalse(repo.getTransaction().isPresent());
            Layer layer = repo.branches().getCurrentBranch().featureService()
                    .getLayer(newLayer.getName());
            assertEquals(3, layer.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            repo.abort();
            throw e;
        }
    }

    private BoundingBox expandBy(double deltaX, double deltaY, BoundingBox bounds) {
        double x1 = bounds.get(0).doubleValue();
        double y1 = bounds.get(1).doubleValue();
        double x2 = bounds.get(2).doubleValue();
        double y2 = bounds.get(3).doubleValue();
        BoundingBox exp = new BoundingBox();
        exp.add(x1 - deltaX);
        exp.add(y1 - deltaY);
        exp.add(x2 + deltaX);
        exp.add(y2 + deltaY);

        return exp;
    }

    private Geometry geom(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private GeogigFeature[] createFeatures(RevisionFeatureType type) throws IOException {
        GeometryFactory gf = new GeometryFactory();
        GeogigFeature[] features = { //
                feature(1, type, gf), //
                feature(2, type, gf), //
                feature(3, type, gf), //
                feature(4, type, gf)//
        };
        return features;
    }

    private GeogigFeature feature(int id, RevisionFeatureType type, GeometryFactory gf) {
        GeogigFeature f = new GeogigFeature("fid-" + id, type);
        f.put("name", "Feature " + id);
        f.put("description", "Description of " + id);
        f.put("height", id + 0.1);
        f.put("location", gf.createPoint(new Coordinate(id, id)));
        return f;
    }

    private Layer createLayer(Repo repo) {
        RevisionFeatureType revType;

        revType = new SimpleTypeBuilder("Poi")//
                .addProperty("name", ValueType.STRING, true)//
                .addProperty("description", ValueType.STRING, true)//
                .addProperty("height", ValueType.DOUBLE, true)//
                .addGeometry("location", ValueType.POINT, true, 4326)//
                .build();

        Layer layer = repo.branches().getCurrentBranch().featureService().createLayer(revType);
        return layer;
    }

    private User createUser(Client adminClient, StoreInfo defaultStore) {
        UserInfoPublicProfile publicProfile = new UserInfoPublicProfile()
                .company("Boundless Spatial Inc");

        UserInfoPrivateProfile privateProfile = new UserInfoPrivateProfile()//
                .fullName("Gabriel Roldan")//
                .emailAddress("gabe@example.geogig.io")//
                .location("Rosario, Santa Fe, Argentina")//
                .defaultStore(new IdentifiedObject().id(defaultStore.getId()));

        UserInfo info = new UserInfo()//
                .identity(userName)//
                .siteAdmin(false)//
                .type(UserType.INDIVIDUAL)//
                .publicProfile(publicProfile)//
                .privateProfile(privateProfile);
        User created = adminClient.users().create(info);
        log("Created user: %s\n", created);
        return created;
    }

    private Store createStore(Client adminClient) throws IOException {
        // create a repository store
        File storesDir = tmp.newFolder("test_store_repos");
        log("Creating store at %s ...\n", storesDir.getAbsolutePath());
        StoreInfo defaultStore = new StoreInfo();
        defaultStore.identity(storeName)//
                .description("Repository store for test users")//
                .enabled(true)//
                .connectionInfo(//
                        new FileStoreInfo()//
                                .directory(storesDir.getAbsolutePath()//
                        )//
        );

        Store createdStore = adminClient.stores().create(defaultStore);
        log("Created repository store %s/%s\n", createdStore.getId(), createdStore.getIdentity());
        return createdStore;
    }
}
