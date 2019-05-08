package org.geogig.web.client.datastore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.geogig.web.client.Client.FeatureStreamFormat;
import org.geogig.web.client.Layer;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.DecoratingFeature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import lombok.Setter;

public class GeogigWebFeatureStore extends ContentFeatureStore
        implements GeogigVersionedFeatureSource {

    private final GeogigWebFeatureSource delegate;

    private @Setter boolean returnFidsOnInsert = true;

    public GeogigWebFeatureStore(ContentEntry entry) throws IOException {
        super(entry, (Query) null);
        delegate = new GeogigWebFeatureSource(entry);
        // {
        // public @Override void setTransaction(Transaction transaction) {
        // super.setTransaction(transaction);
        // // keep this feature store in sync
        // GeogigWebFeatureStore.this.setTransaction(transaction);
        // }
        // };
        super.hints = (Set<Hints.Key>) (Set<?>) delegate.getSupportedHints();
    }

    public @Override void setPreferredStreamFormat(FeatureStreamFormat... preferredOrdered) {
        delegate.setPreferredStreamFormat(preferredOrdered);
    }

    public @Override void setTransaction(Transaction transaction) {
        if (transaction != null && !Transaction.AUTO_COMMIT.equals(transaction)) {
            GeogigWebTransactionState.startTransaction(transaction, getDataStore());
        }
        super.setTransaction(transaction);
        delegate.setTransaction(transaction);
    }

    public @Override GeogigWebDatastore getDataStore() {
        return (GeogigWebDatastore) super.getDataStore();
    }

    protected @Override ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        return delegate.getBounds(query);
    }

    protected @Override int getCountInternal(Query query) throws IOException {
        return delegate.getCount(query);
    }

    protected @Override FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
            Query query) throws IOException {
        return delegate.getReaderInternal(query);
    }

    protected @Override SimpleFeatureType buildFeatureType() throws IOException {
        return delegate.buildFeatureType();
    }

    protected @Override FeatureWriter<SimpleFeatureType, SimpleFeature> getWriterInternal(
            Query query, final int flags) throws IOException {

        // Transaction transaction = getTransaction();
        // Preconditions.checkState(!Transaction.AUTO_COMMIT.equals(transaction),
        // "This FeatureStore does not support auto commit. Use a transaction.");
        throw new UnsupportedOperationException();
    }

    public @Override final List<FeatureId> addFeatures(
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection)
            throws IOException {

        final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
        final List<FeatureId> featureIds = new ArrayList<>();

        Hasher hasher = Hashing.murmur3_32().newHasher();
        hasher.putString(getSchema().getName().getLocalPart(), Charsets.UTF_8);
        hasher.putLong(System.currentTimeMillis());
        hasher.putLong(System.nanoTime());
        final String baseId = hasher.hash().toString();
        final AtomicLong seq = new AtomicLong();
        //@formatter:off
        featureCollection = new DecoratingFeatureCollection<SimpleFeatureType, SimpleFeature>(featureCollection) {
            public @Override FeatureIterator<SimpleFeature> features() {
                return new DecoratingFeatureIterator<SimpleFeature>(super.features()) {
                    public @Override SimpleFeature next() throws NoSuchElementException {
                        SimpleFeature f = super.next();
                        Object USE_PROVIDED_FID = f.getUserData().get(Hints.USE_PROVIDED_FID);
                        String fidOverride = null;
                        if(USE_PROVIDED_FID instanceof Boolean && ((Boolean)USE_PROVIDED_FID).booleanValue()) {
                            final String providedFid = String.valueOf(f.getUserData().get(Hints.PROVIDED_FID));
                            if(providedFid != null ) {
                                if(f.getID().equals(providedFid)) {
                                    return f;
                                }else {
                                    fidOverride = providedFid;
                                }
                            }
                        }
                        if(fidOverride == null) {
                            fidOverride = baseId + seq.incrementAndGet();
                        }
                        final String fid = fidOverride;
                        f = new DecoratingFeature(f) {
                            public @Override FeatureId getIdentifier() {
                                return filterFactory.featureId(fid);
                            }
                            public @Override String getID() {
                                return fid;
                            }                        
                        };
                        if(returnFidsOnInsert) {
                            featureIds.add(f.getIdentifier());
                        }
                        return f;
                    }
                };
            }
        };
        //@formatter:on

        GeogigFeatureCollection geogigFeatures;

        geogigFeatures = GeoToolsDomainBridge.toFeatureCollection(featureCollection);

        Layer layer = delegate.getLayer();
        // String currentLayerTreeHash = layer.getLayerHash();
        layer.addFeatures(geogigFeatures);
        // String updatedTreeHash = layer.getLayerHash();

        return featureIds;
    }

    public @Override void modifyFeatures(Name[] attNames, Object[] attValues, Filter filter) {

        Layer layer = delegate.getLayer();

        List<String> attributes = Arrays.asList(attNames).stream().map((n) -> n.getLocalPart())
                .collect(Collectors.toList());
        List<Object> values = Arrays.asList(attValues);

        FeatureFilter apiFilter = GeoToolsDomainBridge.toFilter(filter);
        layer.modifyFeatures(attributes, values, apiFilter);
    }

    public @Override void removeFeatures(Filter filter) throws IOException {
        Layer layer = delegate.getLayer();
        FeatureFilter apiFilter = GeoToolsDomainBridge.toFilter(filter);
        layer.removeFeatures(apiFilter);
    }
}
