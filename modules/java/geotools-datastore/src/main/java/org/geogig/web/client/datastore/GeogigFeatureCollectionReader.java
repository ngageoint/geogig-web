package org.geogig.web.client.datastore;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.simple.SimpleFeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

public class GeogigFeatureCollectionReader implements SimpleFeatureReader {

    private SimpleFeatureType collectionSchema;

    private GeogigFeatureCollection iterator;

    public GeogigFeatureCollectionReader(GeogigFeatureCollection stream, Name name) {
        this.collectionSchema = GeoToolsDomainBridge.toFeatureType(stream.getFeatureType(), name);
        this.iterator = stream;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return collectionSchema;
    }

    @Override
    public SimpleFeature next()
            throws IOException, IllegalArgumentException, NoSuchElementException {
        return GeoToolsDomainBridge.toFeature(collectionSchema, iterator.next());
    }

    @Override
    public boolean hasNext() throws IOException {
        return iterator.hasNext();
    }

    @Override
    public void close() throws IOException {
        iterator.close();
    }

}
