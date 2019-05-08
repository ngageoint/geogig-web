package org.geogig.web.integration;

import java.io.File;
import java.io.IOException;

import org.geogig.web.client.Client;
import org.geogig.web.client.SimpleTypeBuilder;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.model.FileStoreInfo;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;
import org.geogig.web.model.UserType;
import org.geogig.web.model.ValueType;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class TestSupport extends ExternalResource {

    public TemporaryFolder tmp = new TemporaryFolder();

    private String baseURL = "http://localhost:8181/";

    private Client adminClient;

    private String adminUser = "admin";

    private String adminPassword = "g30g1g";

    private User admin;

    public @Override void before() throws Throwable {
        tmp.create();
        adminClient = new Client(baseURL);
        admin = adminClient.login(adminUser, adminPassword);
    }

    public @Override void after() {
        tmp.delete();
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

    public Client newClient() {
        return new Client(baseURL);
    }

    public Store createStore(String name, String description) {
        File directory;
        try {
            directory = tmp.newFolder(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileStoreInfo connectionInfo = new FileStoreInfo().directory(directory.getAbsolutePath());

        StoreInfo store = new StoreInfo().identity(name).description(description).enabled(true)
                .connectionInfo(connectionInfo);
        Store created = adminClient.stores().create(store);
        return created;
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

    public void deleteUser(String name) {
        adminClient.users().delete(name);
    }

    public void deleteStore(String name) {
        adminClient.stores().delete(name);
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
        GeogigFeature f = new GeogigFeature("fid-" + id, null);
        f.put("name", "Feature " + id);
        f.put("description", "Description of " + id);
        f.put("height", id + 0.1);
        f.put("location", gf.createPoint(new Coordinate(id, id)));
        return f;
    }
}
