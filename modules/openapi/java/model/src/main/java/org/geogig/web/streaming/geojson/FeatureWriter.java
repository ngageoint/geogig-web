package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Objects;

import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.core.JsonGenerator;

public class FeatureWriter {

    private JsonGenerator generator;

    private String defaultGeometry;

    private GeometryWriter geometryWriter;

    public FeatureWriter(JsonGenerator generator, /* nullable */ String defaultGeometry) {
        this.generator = generator;
        this.defaultGeometry = defaultGeometry;
        this.geometryWriter = new GeometryWriter(generator);
    }

    public void write(GeogigFeature feature) throws IOException {
        generator.writeStartObject();
        final String defaultGeometry = findDefaultGeometry(feature);
        final String id = feature.getId();
        generator.writeStringField("type", "Feature");
        generator.writeStringField("id", id);

        if (defaultGeometry != null) {
            Geometry geometry = (Geometry) feature.get(defaultGeometry);
            generator.writeFieldName("geometry");
            geometryWriter.write(geometry, defaultGeometry);
        }
        generator.writeFieldName("properties");
        generator.writeStartObject();
        for (Entry<String, Object> kv : feature.entrySet()) {
            final String attName = kv.getKey();
            if (Objects.equals(defaultGeometry, attName)) {
                continue;
            }
            generator.writeFieldName(attName);
            final Object value = kv.getValue();
            if (value instanceof GeogigFeature) {
                write((GeogigFeature) value);
            } else if (value instanceof Geometry) {
                geometryWriter.write((Geometry) value);
            } else {
                generator.writeObject(value);
            }
        }
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private String findDefaultGeometry(GeogigFeature feature) {
        if (this.defaultGeometry != null) {
            return this.defaultGeometry;
        }
        RevisionFeatureType type = feature.getFeatureType();
        if (type != null) {
            return type.getDefaultGeometry();
        }
        for (Entry<String, Object> e : feature.entrySet()) {
            if (e.getValue() instanceof Geometry) {
                return e.getKey();
            }
        }
        return null;
    }
}
