package org.geogig.web.client.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.Client.FeatureStreamFormat;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Layer;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geogig.web.streaming.GeogigFeatureReader;
import org.geotools.data.FeatureListener;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentEntry;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureReaderIterator;
import org.geotools.feature.collection.AdaptorFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.NonNull;

/**
 * Read only feature source that returns a feature stream of the diffs between its defined
 * {@link Layer} and a provided "old version" identifier.
 *
 */
public class GeogigWebDiffFeatureSource implements GeogigVersionedFeatureSource {

    public static enum ChangeType {
        ADDED, MODIFIED, REMOVED;
    }

    private final Layer __layer;

    private String oldVersion;

    private String newVersion;

    private ContentEntry entry;

    private SimpleFeatureType schema;

    private boolean flattenSchema;

    public GeogigWebDiffFeatureSource(@NonNull ContentEntry entry) {
        this.entry = entry;
        checkArgument(entry.getDataStore() instanceof GeogigWebDatastore);
        String layerName = entry.getTypeName();
        __layer = featureService().getLayer(layerName);
        checkNotNull(__layer);
    }

    public void setFlattenSchema(boolean schemaFlattening) {
        this.flattenSchema = schemaFlattening;
    }

    public void setNewVersion(@Nullable String refSpec) {
        this.newVersion = refSpec;
    }

    public void setOldVersion(@NonNull String refSpec) {
        this.oldVersion = refSpec;
    }

    public Transaction getTransaction() {
        return Transaction.AUTO_COMMIT;
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

    public GeogigWebDatastore getDataStore() {
        return (GeogigWebDatastore) entry.getDataStore();
    }

    public @Override SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    public @Override SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        Query query = new Query(getName().getLocalPart(), filter);
        return getFeatures(query);
    }

    public @Override SimpleFeatureCollection getFeatures(final Query query) throws IOException {
        final Hints hints = query.getHints();
        // hints.remove(Hints.SCREENMAP);
        // query.setFilter(Filter.INCLUDE);
        final @Nullable GeometryFactory providedGeometryFactory;
        providedGeometryFactory = (GeometryFactory) hints.get(Hints.JTS_GEOMETRY_FACTORY);
        if (null != providedGeometryFactory) {
            GeogigFeatureReader.GEOM_FAC.set(providedGeometryFactory);
        }

        return new AdaptorFeatureCollection(null, getSchema()) {

            private final Layer layer = getLayer();

            private final Query gtQuery = query;

            private GeogigFeatureCollectionReader reader;

            private Integer cachedSize;

            private ReferencedEnvelope cachedBounds;

            public @Override int size() {
                if (cachedSize == null) {
                    long size = layer.getSize(apiQuery());
                    cachedSize = Integer.valueOf((int) size);
                }
                return cachedSize.intValue();
            }

            public @Override ReferencedEnvelope getBounds() {
                if (cachedBounds == null) {
                    BoundingBox bounds = layer.getBounds(apiQuery());
                    CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
                    cachedBounds = GeoToolsDomainBridge.toBounds(bounds, crs);
                }
                return cachedBounds;
            }

            protected @Override Iterator<SimpleFeature> openIterator() {
                GeogigFeatureCollection stream = layer.getFeatures(apiQuery());
                reader = new GeogigFeatureCollectionReader(stream, getName());
                return new FeatureReaderIterator<SimpleFeature>(reader);
            }

            protected @Override void closeIterator(Iterator<SimpleFeature> close) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private FeatureQuery apiQuery() {
                final FeatureQuery apiQuery = GeoToolsDomainBridge.toQuery(gtQuery);
                if (flattenSchema) {
                    apiQuery.setFlattenDiffSchema(Boolean.TRUE);
                }
                final String newVersion = GeogigWebDiffFeatureSource.this.newVersion;
                String newHead = newVersion == null ? layer.getHead() : newVersion;
                apiQuery.setHead(newHead);
                apiQuery.setOldHead(oldVersion);
                return apiQuery;
            }
        };
    }

    public @Override Name getName() {
        return schema.getName();
    }

    public @Override ResourceInfo getInfo() {
        return new ResourceInfo() {
            final Set<String> words = new HashSet<String>();
            {
                words.add("features");
                words.add(GeogigWebDiffFeatureSource.this.getSchema().getTypeName());
            }

            public @Override ReferencedEnvelope getBounds() {
                try {
                    return GeogigWebDiffFeatureSource.this.getBounds();
                } catch (IOException e) {
                    return null;
                }
            }

            public CoordinateReferenceSystem getCRS() {
                return GeogigWebDiffFeatureSource.this.getSchema().getCoordinateReferenceSystem();
            }

            public String getDescription() {
                return null;
            }

            public Set<String> getKeywords() {
                return words;
            }

            public String getName() {
                return GeogigWebDiffFeatureSource.this.getSchema().getTypeName();
            }

            public URI getSchema() {
                Name name = GeogigWebDiffFeatureSource.this.getSchema().getName();
                URI namespace;
                try {
                    namespace = new URI(name.getNamespaceURI());
                    return namespace;
                } catch (URISyntaxException e) {
                    return null;
                }
            }

            public String getTitle() {
                Name name = GeogigWebDiffFeatureSource.this.getSchema().getName();
                return name.getLocalPart();
            }

        };

    }

    public @Override QueryCapabilities getQueryCapabilities() {
        return new QueryCapabilities();
    }

    public @Override void addFeatureListener(FeatureListener listener) {
        // TODO Auto-generated method stub

    }

    public @Override void removeFeatureListener(FeatureListener listener) {
        // TODO Auto-generated method stub

    }

    public @Override SimpleFeatureType getSchema() {
        if (this.schema == null) {
            Name name = entry.getName();
            RevisionFeatureType featureType = getLayer().getType();
            SimpleFeatureType diffType;
            if (flattenSchema) {
                diffType = GeoToolsDomainBridge.toFlattenedDiffFeatureType(featureType, name);
            } else {
                diffType = GeoToolsDomainBridge.toDiffFeatureType(featureType, name);
            }
            this.schema = diffType;
        }
        return schema;
    }

    public @Override ReferencedEnvelope getBounds() throws IOException {
        return getBounds(Query.ALL);
    }

    public @Override ReferencedEnvelope getBounds(Query query) throws IOException {
        BoundingBox bounds = getLayer().getBounds(GeoToolsDomainBridge.toQuery(query));

        CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
        ReferencedEnvelope env = new ReferencedEnvelope(crs);
        if (!bounds.isEmpty()) {
            env.expandToInclude(bounds.get(0), bounds.get(1));
            env.expandToInclude(bounds.get(2), bounds.get(3));
        }
        return env;
    }

    public @Override int getCount(Query query) throws IOException {
        int size = (int) getLayer().getSize(GeoToolsDomainBridge.toQuery(query));
        return size;
    }

    public @Override Set<java.awt.RenderingHints.Key> getSupportedHints() {
        Set<java.awt.RenderingHints.Key> hints = new HashSet();
        hints.add(Hints.FEATURE_DETACHED);
        hints.add(Hints.JTS_GEOMETRY_FACTORY);
        hints.add(Hints.JTS_COORDINATE_SEQUENCE_FACTORY);
        hints.add(Hints.SCREENMAP);
        hints.add(Hints.GEOMETRY_SIMPLIFICATION);
        return hints;
    }

}
