package org.geogig.web.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.geogig.web.client.AsyncTask;
import org.geogig.web.client.Branch;
import org.geogig.web.client.Client;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Layer;
import org.geogig.web.client.PullRequest;
import org.geogig.web.client.Repo;
import org.geogig.web.client.SimpleTypeBuilder;
import org.geogig.web.client.Store;
import org.geogig.web.client.Transaction;
import org.geogig.web.client.User;
import org.geogig.web.client.UsersClient;
import org.geogig.web.client.datastore.GeogigWebDiffFeatureSource.ChangeType;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.FileStoreInfo;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.MergeResult;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;
import org.geogig.web.model.UserType;
import org.geogig.web.model.ValueType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.rules.ExternalResource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class IntegrationTestSupport extends ExternalResource {

    private String baseURL = "http://localhost:8181/";

    private Client adminClient;

    private String adminUser = "admin";

    private String adminPassword = "g30g1g";

    private User admin;

    private boolean debugging;

    private static File TESTS_FOLDER;

    private Function<String, Client> clientFactory = url -> new Client(url);

    private List<Client> sessions;

    public Client newClient() {
        Client client = clientFactory.apply(baseURL);
        client.setDebugging(this.debugging);
        sessions.add(client);
        return client;
    }

    public void setClientFactory(@NonNull Function<String, Client> factory) {
        this.clientFactory = factory;
    }

    public void init(int serverPort) {
        this.baseURL = String.format("http://localhost:%d/", serverPort);
        adminClient = clientFactory.apply(baseURL);
        adminClient.setBasicAuth(adminUser, adminPassword);
        admin = adminClient.login();
    }

    public @Override void before() throws Throwable {
        sessions = new ArrayList<>();
    }

    public @Override void after() {
        sessions.forEach(client -> {
            if (client != adminClient) {
                client.dispose();
            }
        });
        sessions.clear();
        try {
            UsersClient usersClient = adminClient.users();
            List<User> users = usersClient.getUsers();
            for (User u : users) {
                List<Repo> repos = u.getRepos();
                for (Repo r : repos) {
                    u.repositories().delete(u.getIdentity(), r.getIdentity());
                }

                adminClient.users().delete(u.getIdentity());
            }
            List<Store> stores = adminClient.stores().getAll();
            for (Store s : stores) {
                adminClient.stores().delete(s.getIdentity());
            }
        } finally {
            adminClient.dispose();
        }
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Client getAdminClient() {
        return adminClient;
    }

    public User getAdmin() {
        return admin;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public Store createStore(String name, String description) {
        File directory = new File(TESTS_FOLDER, "/stores/" + name);
        Preconditions.checkState(directory.exists() || directory.mkdirs());

        FileStoreInfo connectionInfo = new FileStoreInfo().directory(directory.getAbsolutePath());

        StoreInfo store = new StoreInfo().identity(name).description(description).enabled(true)
                .connectionInfo(connectionInfo);
        Store created = adminClient.stores().create(store);
        return created;
    }

    public User createUser(@NonNull String login, @NonNull String defaultStore) {
        return createUser(login, "geo123", login + " FullName", login + "@example.com",
                defaultStore);
    }

    public User createUser(String login, String password, String fullName, String email,
            String defaultStore) {

        IdentifiedObject store = new IdentifiedObject().identity(defaultStore);
        UserInfoPrivateProfile privateProfile = new UserInfoPrivateProfile().defaultStore(store)
                .emailAddress(email).fullName(fullName);
        UserInfo info = new UserInfo().identity(login).siteAdmin(false).type(UserType.INDIVIDUAL)
                .privateProfile(privateProfile);
        User created = adminClient.users().create(info);
        created.setPassword(password);

        User user = newClient().login(login, password);
        return user;
    }

    protected Layer createLayer(Repo repo, RevisionFeatureType featureType) {
        final boolean handleTx = !repo.isTransactionPresent();
        if (handleTx) {
            repo.startTransaction();
        }
        try {
            Branch branch = repo.branches().getCurrentBranch();
            Layer layer = branch.featureService().createLayer(featureType);
            if (handleTx) {
                repo.commitSync("created layer " + featureType.getName(), null);
            }
            return layer;
        } catch (RuntimeException e) {
            if (handleTx) {
                repo.abort();
            }
            throw e;
        }
    }

    public boolean deleteUser(String name) {
        Optional<User> user = adminClient.users().tryGet(name);
        if (user.isPresent()) {
            List<Repo> repos = user.get().getRepos();
            repos.forEach((r) -> adminClient.repositories().delete(r.getOwner().getIdentity(),
                    r.getIdentity()));
            return adminClient.users().delete(name);
        }
        return false;
    }

    public boolean deleteStore(String name) {
        return adminClient.stores().delete(name);
    }

    public Geometry geom(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public RevisionFeatureType poiFeatureType() {
        RevisionFeatureType revType;

        revType = new SimpleTypeBuilder("Poi")//
                .addProperty("name", ValueType.STRING, true)//
                .addProperty("description", ValueType.STRING, true)//
                .addProperty("height", ValueType.DOUBLE, true)//
                .addGeometry("location", ValueType.POINT, true, 4326)//
                .build();
        return revType;
    }

    public RevisionFeatureType roadsFeatureType() {
        RevisionFeatureType revType;

        revType = new SimpleTypeBuilder("Roads")//
                .addProperty("name", ValueType.STRING, true)//
                .addProperty("description", ValueType.STRING, true)//
                .addProperty("height", ValueType.DOUBLE, true)//
                .addGeometry("geometry", ValueType.LINESTRING, true, 4326)//
                .build();
        return revType;
    }

    private GeometryFactory gf = new GeometryFactory();

    public GeogigFeature[] poiFeatures() {

        GeogigFeature[] features = { //
                poiFeature(1), //
                poiFeature(2), //
                poiFeature(3), //
                poiFeature(4)//
        };
        return features;
    }

    public GeogigFeature poiFeature(int id) {
        GeogigFeature f = new GeogigFeature(String.valueOf(id), null);
        f.put("name", "Feature " + id);
        f.put("description", "Description of " + id);
        f.put("height", id + 0.1);
        f.put("location", gf.createPoint(new Coordinate(id, id)));
        return f;
    }

    public Worker<Repo> worker(Repo repo) {
        return new Worker<Repo>(repo);
    }

    public @RequiredArgsConstructor class Worker<R> {
        private final Repo repo;

        private Stack<Object> resultStack = new Stack<>();

        public @SuppressWarnings("unchecked") R get() {
            Object peek = resultStack.peek();
            return (R) peek;
        }

        public <T> T get(Class<T> lastResultType) {
            T res = resultStack.stream().filter(o -> lastResultType.isInstance(o))
                    .map(o -> lastResultType.cast(o)).findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format("No result of type %s found in result stack",
                                    lastResultType.getName())));
            return res;
        }

        public @SuppressWarnings("unchecked") <T> Worker<T> run(Callable<T> cmd) {
            T res;
            try {
                res = cmd.call();
            } catch (Exception e) {
                e.printStackTrace();
                Throwables.throwIfUnchecked(e);
                throw new RuntimeException(e);
            }
            resultStack.push(res);
            return (Worker<T>) this;
        }

        public Worker<Transaction> startTransaction() {
            return run(() -> repo.startTransaction().getTransaction().get());
        }

        public Worker<TransactionInfo> commitTransaction(@Nullable String commitMessage) {
            return run(() -> {
                AsyncTask<TransactionInfo> task = repo.commit(commitMessage).awaitTermination();
                assertTrue(task.isComplete());
                return task.getTransaction();
            });
        }

        public Worker<Branch> branch(@NonNull String originBranch, @NonNull String newBranch) {
            return run(() -> {
                String branchDescription = null;
                Branch branch = repo.branches().createBranch(originBranch, newBranch,
                        branchDescription);
                assertNotNull(branch);
                assertEquals(newBranch, branch.getName());
                return branch;
            });
        }

        public Worker<MergeResult> merge(@NonNull String base, @NonNull String head) {// merge head
                                                                                      // onto base
            return run(() -> {
                AsyncTask<MergeResult> task = repo.branches().merge(base, head).awaitTermination();
                assertNotNull(task);
                assertTrue(task.isComplete());
                assertNotNull(task.getTransaction());
                assertEquals(TransactionStatus.OPEN, task.getTransaction().getStatus());
                return task.getResult();
            });
        }

        public Worker<Branch> checkout(@NonNull String branch) {
            return run(() -> {
                Branch checkedout = repo.branches().checkout(branch);
                assertNotNull(checkedout);
                assertEquals(branch, checkedout.getName());
                return checkedout;
            });
        }

        public Worker<Layer> createLayer(@NonNull RevisionFeatureType type) {
            return run(() -> {
                FeatureServiceClient featureService = repo.branches().getCurrentBranch()
                        .featureService();
                Layer layer = featureService.createLayer(type);
                assertNotNull(layer);
                RevisionFeatureType featureType = layer.getType();
                assertNotNull(featureType);
                assertEquals(type.getProperties(), featureType.getProperties());
                return layer;
            });
        }

        public Worker<FeatureServiceClient> insert(String layer, GeogigFeature... features) {
            return insert(layer, Arrays.asList(features));
        }

        public Worker<FeatureServiceClient> insert(String layer, Iterable<GeogigFeature> features) {
            return run(() -> {
                FeatureServiceClient featureService = repo.branches().getCurrentBranch()
                        .featureService();
                RevisionFeatureType type = featureService.getLayer(layer).getType();
                GeogigFeatureCollection collection = GeogigFeatureCollection.of(type,
                        features.iterator());
                featureService.addFeatures(collection);
                return featureService;
            });
        }

        public Worker<FeatureServiceClient> update(String layer, String attName, Object newValue,
                GeogigFeature... features) {
            List<GeogigFeature> modified = Streams.stream(Arrays.asList(features).iterator())
                    .map(f -> {
                        GeogigFeature c = new GeogigFeature(f);
                        c.put(attName, newValue);
                        return c;
                    }).collect(Collectors.toList());
            return insert(layer, modified);
        }

        public Worker<FeatureServiceClient> delete(String layer, String... featureIds) {
            return run(() -> {
                FeatureServiceClient featureService = repo.branches().getCurrentBranch()
                        .featureService();

                FeatureFilter filter = new FeatureFilter();
                filter.featureIds(Lists.newArrayList(featureIds));
                featureService.removeFeatures(layer, filter);

                long count = featureService.getSize(layer, new FeatureQuery().filter(filter));
                assertEquals(0, count);
                return featureService;
            });
        }

        public Worker<PullRequest> pullRequest(@NonNull String fromBranch, Repo toRepo,
                String toBranch) {
            return run(() -> {
                Branch pullRequestTarget = toRepo.branches().get(toBranch);
                String title = "test pull request";
                String description = "test pull request description";
                PullRequest pr = repo.branches().get(fromBranch).pullRequestTo(pullRequestTarget,
                        title, description);
                assertNotNull(pr);
                return pr;
            });
        }

        public Branch getBranch(@NonNull String branch) {
            return repo.branches().get(branch);
        }
    }

    public Map<String, SimpleFeature> toMap(SimpleFeatureCollection collection) {
        Map<String, SimpleFeature> map = new HashMap<>();
        try (SimpleFeatureIterator features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                map.put(feature.getID(), feature);
            }
        }
        return map;
    }

    public Map<String, GeogigFeature> toMap(GeogigFeatureCollection collection) {
        Map<String, GeogigFeature> map = new HashMap<>();
        collection.forEachRemaining(f -> map.put(f.getId(), f));
        return map;
    }

    public void assertDiffFeature(GeogigFeature diffFeature, @Nullable GeogigFeature expectedLeft,
            @Nullable GeogigFeature expectedRight) {

        assertNotNull(diffFeature);
        int ct = ((Integer) diffFeature.get("geogig.changeType")).intValue();
        ChangeType changeType = ChangeType.values()[ct];
        GeogigFeature left = (GeogigFeature) diffFeature.get("old");
        GeogigFeature right = (GeogigFeature) diffFeature.get("new");
        switch (changeType) {
        case ADDED:
            assertNull(left);
            assertFeature(right, expectedRight);
            break;
        case MODIFIED:
            assertFeature(left, expectedLeft);
            assertFeature(right, expectedRight);
            break;
        case REMOVED:
            assertNull(right);
            assertFeature(left, expectedLeft);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    public void assertDiffFeature(SimpleFeature diffFeature, @Nullable GeogigFeature expectedLeft,
            @Nullable GeogigFeature expectedRight) {

        assertDiffFeature(GeoToolsDomainBridge.toFeature(diffFeature), expectedLeft, expectedRight);
    }

    private void assertFeature(GeogigFeature actual, GeogigFeature expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.entrySet(), actual.entrySet());
    }

    static {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        File directory = new File(tmp, "/geogig-web");
        if (directory.exists()) {
            deleteRecursive(directory);
        }
        directory.mkdir();
        new File(directory, "config").mkdir();
        new File(directory, "stores").mkdir();
        TESTS_FOLDER = directory;
    }

    private static void deleteRecursive(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                deleteRecursive(each);
            }
        }
        file.delete();
    }

}
