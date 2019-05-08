package org.geogig.web.integration.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.client.datastore.GeogigWebDatastore;
import org.geogig.web.client.datastore.GeogigWebFeatureStore;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.geogig.web.integration.IntegrationTestSupport;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.FeatureIteratorIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class GeogigWebFeatureSourceTest extends AbstractIntegrationTest {

    //@formatter:off
    private static final String USER = "gabe";
    private static final String PASSWORD = "s3cr3t";
    private static final String REPOSITORY = "naturalEearth";
    //@formatter:on

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private Store store;

    private User user;

    private Repo repo;

    /**
     * {@code master} has {@link IntegrationTestSupport#poiFeatures() poi features} 1 and 2, branch
     * has poi features 1, 3, 4.
     */
    private GeogigWebFeatureStore poiMaster, poinBranch;

    public @Rule ExpectedException ex = ExpectedException.none();

    private Layer poi;

    private String layername;

    private GeogigFeature[] features;

    public @Before void before() throws IOException {
        String methodName = testName.getMethodName();
        String storeName = methodName + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser(USER + "_" + methodName, PASSWORD, "Gabriel Roldan",
                "gabe@example.com", storeName);

        repo = user.createRepo(REPOSITORY);
        poi = createPoiLayer(repo);
        layername = poi.getName();
        {
            RevisionFeatureType type = poi.getType();
            features = testSupport.poiFeatures();

            Repo tx = repo.startTransaction();
            Branch branch = tx.branches().getCurrentBranch().branch("branch");
            tx.commit();

            tx = repo.startTransaction();
            Branch masterBranch = tx.branches().get("master");

            masterBranch.featureService()
                    .addFeatures(GeogigFeatureCollection.of(type, features[0], features[1]));
            tx.commit();

            tx = repo.startTransaction();
            branch = branch.checkout();
            branch.featureService().addFeatures(
                    GeogigFeatureCollection.of(type, features[0], features[2], features[3]));
            tx.commit();

            assertEquals(2, Iterators.size(masterBranch.featureService().getFeatures(layername)));
            assertEquals(3, Iterators.size(branch.featureService().getFeatures(layername)));
        }
        poiMaster = (GeogigWebFeatureStore) new GeogigWebDatastore(repo.branches().get("master"))
                .getFeatureSource(layername);

        poinBranch = (GeogigWebFeatureStore) new GeogigWebDatastore(repo.branches().get("branch"))
                .getFeatureSource(layername);

        createRoadsLayer(repo);
    }

    public @Test void getFeatures() throws IOException {
        assertFeatures(poiMaster.getFeatures(), features[0], features[1]);
        assertFeatures(poinBranch.getFeatures(), features[0], features[2], features[3]);
    }

    public @Test void testInsertRespectsProvidedFeatureIds() throws IOException {

        SimpleFeatureType type = GeoToolsDomainBridge.toFeatureType(poi.getType());
        ListFeatureCollection insert = new ListFeatureCollection(type);
        IntStream.range(100, 110).forEach(i -> {
            SimpleFeature feature = GeoToolsDomainBridge.toFeature(type, testSupport.poiFeature(i));
            assertEquals(Boolean.TRUE, feature.getUserData().get(Hints.USE_PROVIDED_FID));
            insert.add(feature);
        });

        Transaction gttx = new DefaultTransaction();
        poiMaster.setTransaction(gttx);
        List<FeatureId> addedFids = poiMaster.addFeatures((SimpleFeatureCollection) insert);
        gttx.commit();
        gttx.close();
        poiMaster.setTransaction(Transaction.AUTO_COMMIT);

        ListFeatureCollection list = new ListFeatureCollection(poiMaster.getFeatures());
        assertEquals(2 + insert.size(), list.size());
        Set<String> expected = DataUtilities.fidSet(insert);
        Set<String> actual = DataUtilities.fidSet(list);
        assertEquals(expected, Sets.intersection(expected, actual));
    }

    // @Ignore // inserts are not respecting provided fids yet
    // public @Test void getFeaturesFidFilter() throws IOException {
    // Filter filter;
    // Query query;
    //
    // filter = FF.id(FF.featureId(features[0].getId()));
    // query = new Query(layername, filter);
    // assertFeatures(master.getFeatures(query), features[0]);
    //
    // filter = FF.id(FF.featureId(features[2].getId()), FF.featureId(features[3].getId()));
    // query = new Query(layername, filter);
    // assertFeatures(branch.getFeatures(query), features[3], features[2]);
    // }

    public @Test void getFeaturesBoundsFilter() throws IOException {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();
        final String geom = poi.getType().getDefaultGeometry();

        Filter filter;

        // filter = FF.bbox(FF.property("@bounds"), boundsOf(crs, features[0]));
        filter = FF.bbox(FF.property(geom), boundsOf(crs, features[0]));
        ContentFeatureCollection actual = poiMaster.getFeatures(new Query(layername, filter));
        assertFeatures(actual, features[0]);

        // filter = FF.bbox(FF.property("@bounds"), boundsOf(crs, features[2], features[3]));
        filter = FF.bbox(FF.property(geom), boundsOf(crs, features[2], features[3]));
        actual = poinBranch.getFeatures(new Query(layername, filter));
        assertFeatures(actual, features[2], features[3]);
    }

    public @Test void getFeaturesBoundsFilter2() throws IOException {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();
        final String geom = poi.getType().getDefaultGeometry();

        Filter filter;

        ReferencedEnvelope bounds = boundsOf(crs, features[0]);
        String cql = String.format("BBOX(\"@bounds\", %f,%f,%f,%f)", bounds.getMinX(),
                bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());

        filter = FF.bbox(FF.property(geom), bounds);
        ContentFeatureCollection actual = poiMaster.getFeatures(new Query(layername, filter));
        assertFeatures(actual, features[0]);

        filter = FF.bbox(FF.property(geom), boundsOf(crs, features[2], features[3]));
        actual = poinBranch.getFeatures(new Query(layername, filter));
        assertFeatures(actual, features[2], features[3]);
    }

    public @Test void getFeaturesReprojection() throws Exception {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();

        Query query = new Query();
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:3857");
        query.setCoordinateSystemReproject(targetCrs);

        ContentFeatureCollection reprojected = poiMaster.getFeatures(query);
        assertEquals(targetCrs, reprojected.getSchema().getCoordinateReferenceSystem());
    }

    public @Test void getBounds() throws IOException {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();
        ReferencedEnvelope expected;

        expected = boundsOf(crs, features[0], features[1]);
        assertEquals(expected, poiMaster.getBounds());

        expected = boundsOf(crs, features[0], features[2], features[3]);
        assertEquals(expected, poinBranch.getBounds());
    }

    // @Ignore // inserts are not respecting provided fids yet
    // public @Test void getBoundsFidFilter() throws IOException {
    // CoordinateReferenceSystem crs = master.getSchema().getCoordinateReferenceSystem();
    // ReferencedEnvelope expected;
    //
    // Filter filter;
    //
    // filter = FF.id(FF.featureId(features[0].getId()));
    // expected = boundsOf(crs, features[0]);
    // assertEquals(expected, master.getBounds(new Query(layername, filter)));
    //
    // filter = FF.id(FF.featureId(features[3].getId()));
    // expected = boundsOf(crs, features[3]);
    // assertEquals(expected, master.getBounds(new Query(layername, filter)));
    // }

    public @Test void getBoundsBoundsFilter() throws IOException {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();

        final String geom = poi.getType().getDefaultGeometry();

        ReferencedEnvelope expected;

        Filter filter;

        expected = boundsOf(crs, features[0]);
        filter = FF.bbox(FF.property(geom), expected);
        assertEquals(expected, poiMaster.getBounds(new Query(layername, filter)));

        expected = boundsOf(crs, features[3]);
        filter = FF.bbox(FF.property(geom), expected);
        assertEquals(expected, poinBranch.getBounds(new Query(layername, filter)));
    }

    public @Test void getCount() throws IOException {
        assertEquals(2, poiMaster.getCount(Query.ALL));
        assertEquals(3, poinBranch.getCount(Query.ALL));
    }

    @Ignore // inserts are not respecting provided fids yet
    public @Test void getCountFidFilter() throws IOException {
        Filter filter;

        filter = FF.id(FF.featureId(features[0].getId()));
        assertEquals(1, poiMaster.getCount(new Query(layername, filter)));

        filter = FF.id(FF.featureId(features[3].getId()));
        assertEquals(1, poinBranch.getCount(new Query(layername, filter)));
    }

    public @Test void getCountBoundsFilter() throws IOException {
        CoordinateReferenceSystem crs = poiMaster.getSchema().getCoordinateReferenceSystem();
        final String geom = poi.getType().getDefaultGeometry();

        Filter filter;

        filter = FF.bbox(FF.property(geom), boundsOf(crs, features[0]));
        assertEquals(1, poiMaster.getCount(new Query(layername, filter)));

        filter = FF.bbox(FF.property(geom), boundsOf(crs, features[2], features[3]));
        assertEquals(2, poinBranch.getCount(new Query(layername, filter)));
    }

    private void assertFeatures(ContentFeatureCollection collection, GeogigFeature... expected) {
        assertNotNull(collection);
        List<SimpleFeature> features = toList(collection);
        assertFeatures(features, expected);
    }

    private void assertFeatures(List<SimpleFeature> actual, GeogigFeature... expected) {
        assertEquals(expected.length, actual.size());
    }

    private List<SimpleFeature> toList(ContentFeatureCollection collection) {
        List<SimpleFeature> features;
        features = Lists.newArrayList(new FeatureIteratorIterator<>(collection.features()));
        return features;
    }

    ReferencedEnvelope boundsOf(CoordinateReferenceSystem crs, GeogigFeature... features) {
        ReferencedEnvelope e = new ReferencedEnvelope(crs);
        Lists.newArrayList(features).forEach((f) -> {
            BoundingBox bounds = f.getBounds();
            Envelope envelope = GeoToolsDomainBridge.toBounds(bounds);
            e.expandToInclude(envelope);
        });
        return e;
    }
}
