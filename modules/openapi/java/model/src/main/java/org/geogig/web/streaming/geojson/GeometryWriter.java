package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.core.JsonGenerator;

public class GeometryWriter {

    private JsonGenerator generator;

    public GeometryWriter(JsonGenerator generator) {
        this.generator = generator;
    }

    public void write(Geometry geometry) throws IOException {
        write(geometry, null);
    }

    public void write(Geometry geometry, String customNameProperty) throws IOException {
        if (geometry == null) {
            generator.writeNull();
            return;
        }
        generator.writeStartObject();
        generator.writeStringField("type", geometry.getGeometryType());
        if (customNameProperty != null) {
            generator.writeStringField("name", customNameProperty);
        }
        generator.writeFieldName("coordinates");
        writeGeometry(geometry, generator);
        generator.writeEndObject();
    }

    private void writeGeometry(Geometry geometry, JsonGenerator generator) throws IOException {
        if (geometry instanceof GeometryCollection) {
            writeMultiGeom((GeometryCollection) geometry, generator);
        } else {
            writeSimpleGeom(geometry, generator);
        }
    }

    private void writeMultiGeom(GeometryCollection multi, JsonGenerator generator)
            throws IOException {
        generator.writeStartArray();
        for (int i = 0; i < multi.getNumGeometries(); i++) {
            writeGeometry(multi.getGeometryN(i), generator);
        }
        generator.writeEndArray();
    }

    private void writeSimpleGeom(Geometry geometry, JsonGenerator generator) throws IOException {
        if (geometry instanceof Point) {
            Point p = (Point) geometry;
            writeCoordinate(p.getX(), p.getY(), generator);
        } else if (geometry instanceof Polygon) {
            Polygon poly = (Polygon) geometry;
            generator.writeStartArray();
            writeCoordinateSequence(poly.getExteriorRing(), generator);
            for (int r = 0; r < poly.getNumInteriorRing(); r++) {
                writeCoordinateSequence(poly.getInteriorRingN(r), generator);
            }
            generator.writeEndArray();
        } else {
            writeCoordinateSequence(geometry, generator);
        }
    }

    private void writeCoordinateSequence(Geometry simpleGeom, JsonGenerator generator)
            throws IOException {
        final AtomicReference<CoordinateSequence> seqRef = new AtomicReference<>();
        simpleGeom.apply(new CoordinateSequenceFilter() {
            public @Override void filter(CoordinateSequence seq, int i) {
                seqRef.set(seq);
            }

            //@formatter:off
            public @Override boolean isGeometryChanged() {return false;}
            public @Override boolean isDone() {return true;}
            //@formatter:on
        });

        CoordinateSequence seq = seqRef.get();
        int size = seq.size();
        generator.writeStartArray();
        for (int i = 0; i < size; i++) {
            writeCoordinate(seq, i, generator);
        }
        generator.writeEndArray();
    }

    private void writeCoordinate(CoordinateSequence seq, int i, JsonGenerator generator)
            throws IOException {
        double x = seq.getOrdinate(i, 0);
        double y = seq.getOrdinate(i, 1);
        writeCoordinate(x, y, generator);
    }

    private void writeCoordinate(double x, double y, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        generator.writeNumber(x);
        generator.writeNumber(y);
        generator.writeEndArray();
    }

}
