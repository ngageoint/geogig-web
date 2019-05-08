package org.geogig.web.streaming;

import java.io.IOException;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

public interface GeogigFeatureReader {

    GeometryFactory DEFAULT_GEOMETRY_FACTORY = new GeometryFactory(
            new PackedCoordinateSequenceFactory());

    ThreadLocal<GeometryFactory> GEOM_FAC = new ThreadLocal<GeometryFactory>() {
        protected @Override GeometryFactory initialValue() {
            return DEFAULT_GEOMETRY_FACTORY;
        }
    };

    org.geogig.web.model.Feature read() throws IOException;

}