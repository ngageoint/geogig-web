package org.geogig.web.client.datastore;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Client;
import org.geogig.web.client.Client.FeatureStreamFormat;
import org.geogig.web.client.Repo;
import org.geogig.web.client.User;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeogigWebDataStoreFactory implements DataStoreFactorySpi {

    public static final Param BASE_URL = new Param("geogig_web_base_url", URL.class,
            "Base URL for the remote server's geogig web API", true, "http://localhost:8080");

    public static final Param REPO_OWNER = new Param("repository_owner", String.class,
            "Login name for the owner of the repository", true);

    public static final Param REPO_NAME = new Param("repository_name", String.class,
            "Name of the repository", true);

    public static final Param REPO_BRANCH = new Param("repository_branch", String.class,
            "Branch to work against", false);

    public static final Param HTTP_USER = new Param("basic_auth_user", String.class,
            "User name to connect as to the remote server using HTTP Basic Auth", false);

    public static final Param HTTP_PASSWORD = new Param("basic_auth_password", String.class,
            "User password to connect to the remote server using HTTP Basic Auth", false, null,
            Collections.singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));

    public static final Param DEFAULT_NAMESPACE = new Param("namespace", String.class,
            "Optional namespace for feature types. Used by GeoServer.", false);

    public @Override String getDisplayName() {
        return "GeoGig Web";
    }

    public @Override String getDescription() {
        return "Connects to GeoGig Web API FeatureService";
    }

    public @Override Param[] getParametersInfo() {
        return new Param[] { BASE_URL, HTTP_USER, HTTP_PASSWORD, REPO_OWNER, REPO_NAME, REPO_BRANCH,
                DEFAULT_NAMESPACE };
    }

    public @Override boolean canProcess(Map<String, Serializable> params) {
        boolean canProcess = DataUtilities.canProcess(params, getParametersInfo());
        return canProcess;
    }

    public @Override boolean isAvailable() {
        return true;
    }

    public @Override Map<Key, ?> getImplementationHints() {
        return null;
    }

    public @Override DataStore createDataStore(Map<String, Serializable> params)
            throws IOException {

        URL baseURL = (URL) BASE_URL.lookUp(params);
        String user = (String) HTTP_USER.lookUp(params);
        String pwd = (String) HTTP_PASSWORD.lookUp(params);

        Client geogigClient = new Client(baseURL.toString());
        geogigClient.setPreferredFeatureStreamFormat(//
                //FeatureStreamFormat.SIMPLIFIED_GEOJSON_BINARY, //
                FeatureStreamFormat.GEOJSON_BINARY, //
                //FeatureStreamFormat.SIMPLIFIED_GEOJSON, //
                FeatureStreamFormat.GEOJSON//
        );

        geogigClient.setBasicAuth(user, pwd);
        try {
            User authenticatedUser = geogigClient.login();
            String u = authenticatedUser.getFullName() == null ? user
                    : authenticatedUser.getFullName();
            log.debug("Connected to {} as {}, creating DataStore", baseURL, u);
        } catch (RuntimeException e) {
            Throwable cause = Throwables.getRootCause(e);
            log.debug("Unable to connect to {} as {}", baseURL, user, cause);
            throw new IOException(String.format("Unable to connect to %s as %s", baseURL, user),
                    cause);
        }
        String repoOwner = (String) REPO_OWNER.lookUp(params);
        String repoName = (String) REPO_NAME.lookUp(params);
        String namespaceURI = (String) DEFAULT_NAMESPACE.lookUp(params);

        User owner = geogigClient.users().get(repoOwner);

        Repo repoClient = owner.getRepo(repoName);
        String branch = (String) REPO_BRANCH.lookUp(params);

        GeogigWebDatastore datastore;
        if (branch == null) {
            datastore = new GeogigWebDatastore(repoClient);
        } else {
            Branch apiBranch = repoClient.branches().get(branch);
            datastore = new GeogigWebDatastore(apiBranch);
        }
        if (!Strings.isNullOrEmpty(namespaceURI)) {
            datastore.setNamespaceURI(namespaceURI);
        }
        return datastore;
    }

    public @Override DataStore createNewDataStore(Map<String, Serializable> params)
            throws IOException {
        throw new UnsupportedOperationException();
    }

}
