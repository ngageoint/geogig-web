package org.geogig.web.integration;

import java.util.function.Function;

import org.geogig.server.app.GeogigServerApplication;
import org.geogig.web.client.Client;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

//@formatter:off
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {GeogigServerApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(
        inheritProperties = true,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "GEOGIG_SERVER_CONFIG_DIRECTORY=${java.io.tmpdir}/geogig-web/config"
                }
)
//@formatter:on
public abstract class AbstractIntegrationTest {

    public @Rule IntegrationTestSupport testSupport = new IntegrationTestSupport();

    private @LocalServerPort int port;

    public @Rule TestName testName = new TestName();

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void initPort() {
        testSupport.setClientFactory(getClientFactory());
        testSupport.init(port);
    }

    protected Function<String, Client> getClientFactory() {
        return url -> new Client(url);
    }

    protected Layer createPoiLayer(Repo repo) {
        return createLayer(repo, testSupport.poiFeatureType());
    }

    protected Layer createRoadsLayer(Repo repo) {
        return createLayer(repo, testSupport.roadsFeatureType());
    }

    protected Layer createLayer(Repo repo, RevisionFeatureType featureType) {
        return testSupport.createLayer(repo, featureType);
    }

    protected GeogigFeature poi(int index) {
        return testSupport.poiFeature(index);
    }

}
