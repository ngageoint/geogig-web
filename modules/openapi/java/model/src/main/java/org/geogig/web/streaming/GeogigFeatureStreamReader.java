package org.geogig.web.streaming;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.geogig.web.model.GeogigFeature;

public interface GeogigFeatureStreamReader {

    default void close() {
        GeogigFeatureReader.GEOM_FAC.remove();
    }

    Optional<GeogigFeature> tryNext() throws IOException;

    static GeogigFeatureStreamReader of(GeogigFeature... features) {
        return GeogigFeatureStreamReader.of(Arrays.asList(features).iterator());
    }

    static GeogigFeatureStreamReader of(List<GeogigFeature> features) {
        return GeogigFeatureStreamReader.of(features.iterator());
    }

    static GeogigFeatureStreamReader of(final Iterator<GeogigFeature> features) {
        return new GeogigFeatureStreamReader() {
            private Iterator<GeogigFeature> iterator = features;

            public @Override Optional<GeogigFeature> tryNext() throws IOException {
                GeogigFeature next = iterator != null && iterator.hasNext() ? next = iterator.next()
                        : null;
                return Optional.ofNullable(next);
            }

            public @Override void close() {
                iterator = null;
            }
        };
    }
}