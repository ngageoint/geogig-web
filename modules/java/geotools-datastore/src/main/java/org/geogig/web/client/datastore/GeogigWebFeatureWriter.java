package org.geogig.web.client.datastore;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureWriter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GeogigWebFeatureWriter implements SimpleFeatureWriter {

    private SimpleFeatureReader reader;

    private GeogigWebFeatureWriter(SimpleFeatureReader reader) {
        this.reader = reader;
    }

    public static GeogigWebFeatureWriter create(final SimpleFeatureReader reader) {
        return new GeogigWebFeatureWriter(reader);
    }

    public static GeogigWebFeatureWriter createAppendable(SimpleFeatureReader reader) {
        return new GeogigWebFeatureWriter(new InfiniteFeatureReader(reader));
    }

    public @Override SimpleFeatureType getFeatureType() {
        return reader.getFeatureType();
    }

    public @Override boolean hasNext() throws IOException {
        return reader.hasNext();
    }

    public @Override SimpleFeature next() throws IOException {
        return reader.next();
    }

    public @Override void remove() throws IOException {
        // TODO Auto-generated method stub

    }

    public @Override void write() throws IOException {
        // TODO Auto-generated method stub

    }

    public @Override void close() throws IOException {
        // TODO Auto-generated method stub

    }

    private static final class InfiniteFeatureReader implements SimpleFeatureReader {

        private FeatureReader<SimpleFeatureType, SimpleFeature> reader;

        private SimpleFeatureBuilder featureBuilder;

        public InfiniteFeatureReader(FeatureReader<SimpleFeatureType, SimpleFeature> reader) {
            this.reader = reader;
            this.featureBuilder = new SimpleFeatureBuilder(reader.getFeatureType());
        }

        public @Override boolean hasNext() throws IOException {
            return reader.hasNext();
        }

        public @Override SimpleFeatureType getFeatureType() {
            return reader.getFeatureType();
        }

        public @Override SimpleFeature next() throws IOException {
            if (reader.hasNext()) {
                return reader.next();
            }
            SimpleFeature feature = this.featureBuilder.buildFeature(null);
            return feature;
        }

        public @Override void close() throws IOException {
            reader.close();
        }
    }
}
