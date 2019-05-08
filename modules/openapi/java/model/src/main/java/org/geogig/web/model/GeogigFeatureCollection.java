package org.geogig.web.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.geogig.web.streaming.GeogigFeatureStreamReader;
import org.geogig.web.streaming.StreamingResponse;

import lombok.Getter;
import lombok.NonNull;

public class GeogigFeatureCollection extends org.geogig.web.model.FeatureCollection
        implements CloseableIterator<GeogigFeature>, StreamingResponse {

    private GeogigFeatureStreamReader featureReader;

    private Optional<GeogigFeature> next;

    //@formatter:off
    private static Runnable NO_OP = new Runnable() {public @Override void run() {}};
    //@formatter:on

    private Runnable additionalCloseTask = NO_OP;

    private Long size;

    private BoundingBox bounds;

    private @Getter Map<String, String> extraData = new HashMap<>();

    private GeogigFeatureCollection(/* Nullable */ RevisionFeatureType type,
            @NonNull GeogigFeatureStreamReader featureReader) throws IOException {
        this.setFeatureType(type);
        this.featureReader = featureReader;
        this.next = featureReader.tryNext();
    }

    private GeogigFeatureCollection(long sizeOnly) {
        this.size = Long.valueOf(sizeOnly);
        this.next = Optional.empty();
    }

    private GeogigFeatureCollection(@NonNull BoundingBox boundsOnly) {
        this.bounds = boundsOnly;
        this.next = Optional.empty();
    }

    public Optional<BoundingBox> getBounds() {
        return Optional.ofNullable(bounds);
    }

    public Optional<Long> getSize() {
        return Optional.ofNullable(size);
    }

    public void setSize(long size) {
        this.size = Long.valueOf(size);
    }

    public void setBounds(@NonNull BoundingBox bounds) {
        this.bounds = bounds;
    }

    @Override
    public boolean hasNext() {
        return next.isPresent();
    }

    private Function<GeogigFeature, GeogigFeature> transform = (f) -> f;

    @Override
    public GeogigFeature next() {
        if (next.isPresent()) {
            GeogigFeature current = next.get();
            try {
                next = featureReader.tryNext();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return transform.apply(current);
        }
        close();
        throw new NoSuchElementException();
    }

    public void onNext(@NonNull Function<GeogigFeature, GeogigFeature> transform) {
        this.transform = transform;
    }

    public @Override void close() {
        try {
            if (featureReader != null) {
                featureReader.close();
                featureReader = null;
            }
        } finally {
            additionalCloseTask.run();
            additionalCloseTask = NO_OP;
        }
    }

    @Override
    public void onClose(Runnable additionalCloseTask) {
        this.additionalCloseTask = additionalCloseTask;
    }

    public static GeogigFeatureCollection of(GeogigFeature... features) {
        GeogigFeatureStreamReader reader = GeogigFeatureStreamReader.of(features);
        try {
            return new GeogigFeatureCollection(null, reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GeogigFeatureCollection of(RevisionFeatureType type, GeogigFeature... features) {
        return GeogigFeatureCollection.of(type, Arrays.asList(features).iterator());
    }

    public static GeogigFeatureCollection of(RevisionFeatureType feautreType,
            Iterator<GeogigFeature> features) {
        GeogigFeatureStreamReader reader = GeogigFeatureStreamReader.of(features);
        try {
            return new GeogigFeatureCollection(feautreType, reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GeogigFeatureCollection streamingCollection(//
            RevisionFeatureType type, //
            GeogigFeatureStreamReader featureReader) throws IOException {

        return new GeogigFeatureCollection(type, featureReader);
    }

    public static GeogigFeatureCollection sizeOnly(long size) {
        return new GeogigFeatureCollection(size);
    }

    public static GeogigFeatureCollection boundsOnly(@NonNull BoundingBox bounds) {
        return new GeogigFeatureCollection(bounds);
    }
}
