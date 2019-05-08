package org.geogig.web.model.geotools;

import java.io.IOException;
import java.util.Optional;

import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.streaming.GeogigFeatureStreamReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GeogigFeatureReaderGeotoolsAdapter implements GeogigFeatureStreamReader {

    private final RevisionFeatureType geogigFeatureType;

    private FeatureIterator<SimpleFeature> features;

    private String typeIdPrefix = null;

    public GeogigFeatureReaderGeotoolsAdapter(
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {

        SimpleFeatureType schema = featureCollection.getSchema();
        this.geogigFeatureType = GeoToolsDomainBridge.toFeatureType(schema);
        this.features = featureCollection.features();
    }

    public RevisionFeatureType getType() {
        return geogigFeatureType;
    }

    public @Override void close() {
        FeatureIterator<SimpleFeature> features = this.features;
        this.features = null;
        if (features != null) {
            features.close();
        }
    }

    public @Override Optional<GeogigFeature> tryNext() throws IOException {
        if (features.hasNext()) {
            SimpleFeature next = features.next();
            if (typeIdPrefix == null) {
                String prefix = next.getType().getTypeName() + ".";
                String id = next.getID();
                if (id.startsWith(prefix)) {
                    this.typeIdPrefix = prefix;
                } else {
                    this.typeIdPrefix = "";
                }
            }

            GeogigFeature geogigFeature = GeoToolsDomainBridge.toFeature(geogigFeatureType, next,
                    typeIdPrefix);
            return Optional.of(geogigFeature);
        }
        close();
        return Optional.empty();
    }

}
