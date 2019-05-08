package org.geogig.web.client.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.Client.FeatureStreamFormat;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Layer;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geogig.web.streaming.GeogigFeatureReader;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.Hints;
import org.geotools.factory.Hints.Key;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Throwables;

public class GeogigWebFeatureSource extends ContentFeatureSource
        implements GeogigVersionedFeatureSource {

    private final Layer __layer;

    private String headRefSpec;

    public GeogigWebFeatureSource(ContentEntry entry) throws IOException {
        this(entry, null);
    }

    public GeogigWebFeatureSource(ContentEntry entry, @Nullable String headRefSpec) throws IOException {
        super(entry, Query.ALL);
        this.headRefSpec = headRefSpec;
        checkArgument(entry.getDataStore() instanceof GeogigWebDatastore);

        String layerName = entry.getTypeName();
        try {
            __layer = featureService().getLayer(layerName);
        } catch (NoSuchElementException e) {
            throw new IOException(layerName + " does not exist", Throwables.getRootCause(e));
        }
        checkNotNull(__layer);
    }

    Layer getLayer() {
        Transaction transaction = getTransaction();
        Layer layer = GeogigWebTransactionState.resolveLayer(transaction, __layer);
        return layer;
    }

    public @Override void setPreferredStreamFormat(FeatureStreamFormat... preferredOrdered) {
        getDataStore().getClient().setPreferredFeatureStreamFormat(preferredOrdered);
    }

    FeatureServiceClient featureService() {
        return getDataStore().getBranch().featureService();
    }

    protected @Override void addHints(Set<Key> hints) {
        hints.add(Hints.FEATURE_DETACHED);
        hints.add(Hints.JTS_GEOMETRY_FACTORY);
        hints.add(Hints.JTS_COORDINATE_SEQUENCE_FACTORY);
        hints.add(Hints.SCREENMAP);
        hints.add(Hints.GEOMETRY_SIMPLIFICATION);
    }

    public @Override GeogigWebDatastore getDataStore() {
        return (GeogigWebDatastore) super.getDataStore();
    }

    protected @Override boolean canReproject() {
        return false;
    }

    protected @Override boolean canLimit() {
        return false;
    }

    protected @Override boolean canOffset() {
        return false;
    }

    protected @Override boolean canFilter() {
        return false;
    }

    protected @Override boolean canRetype() {
        return false;
    }

    protected @Override boolean canSort() {
        return false;
    }

    protected @Override boolean canTransact() {
        return false;
    }

    protected @Override boolean canEvent() {
        return false;
    }

    protected @Override ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        BoundingBox bounds = getLayer().getBounds(GeoToolsDomainBridge.toQuery(query));

        CoordinateReferenceSystem crs = getAbsoluteSchema().getCoordinateReferenceSystem();
        ReferencedEnvelope env = new ReferencedEnvelope(crs);
        if (!bounds.isEmpty()) {
            env.expandToInclude(bounds.get(0), bounds.get(1));
            env.expandToInclude(bounds.get(2), bounds.get(3));
        }
        return env;
    }

    protected @Override int getCountInternal(Query query) throws IOException {
        int size;
        size = (int) getLayer().getSize(GeoToolsDomainBridge.toQuery(query));
        return size;
    }

    protected @Override SimpleFeatureType buildFeatureType() throws IOException {
        Name name = getEntry().getName();
        SimpleFeatureType ft = GeoToolsDomainBridge.toFeatureType(getLayer().getType(), name);
        return ft;
    }

    protected @Override FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
            Query query) throws IOException {

        final String head = this.headRefSpec;
        final Hints hints = query.getHints();
        final @Nullable GeometryFactory providedGeometryFactory;
        providedGeometryFactory = (GeometryFactory) hints.get(Hints.JTS_GEOMETRY_FACTORY);
        if (null != providedGeometryFactory) {
            GeogigFeatureReader.GEOM_FAC.set(providedGeometryFactory);
        }

        FeatureQuery apiQuery = GeoToolsDomainBridge.toQuery(query);
        apiQuery.setHead(head);
        Layer layer = getLayer();
        GeogigFeatureCollection stream = layer.getFeatures(apiQuery);
        return new GeogigFeatureCollectionReader(stream, getName());
    }

}
