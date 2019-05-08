package org.geogig.web.streaming.geojson;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class GeometryReader {

    private GeometryFactory geometryFactory;

    public GeometryReader(GeometryFactory geometryFactory) {
        Objects.requireNonNull(geometryFactory);
        this.geometryFactory = geometryFactory;
    }

    public Geometry parse(ObjectNode geometryNode) {
        final String type = geometryNode.findValue("type").asText();
        ArrayNode coordinates = (ArrayNode) geometryNode.findValue("coordinates");
        switch (type) {
        case "Point":
            return parsePoint(coordinates);
        case "MultiPoint":
            return parseMultiPoint(coordinates);
        case "LineString":
            return parseLineString(coordinates);
        case "MultiLineString":
            return parseMultiLineString(coordinates);
        case "Polygon":
            return parsePolygon(coordinates);
        case "MultiPolygon":
            return parseMultiPolygon(coordinates);
        default:
            throw new IllegalArgumentException("Uknown geometry node: " + geometryNode.toString());
        }
    }

    private MultiLineString parseMultiLineString(ArrayNode coordinates) {
        LineString[] lineStrings = parseGeometries(coordinates, LineString.class,
                this::parseLineString);
        return geometryFactory.createMultiLineString(lineStrings);
    }

    private MultiPolygon parseMultiPolygon(ArrayNode coordinates) {
        Polygon[] polygons = parseGeometries(coordinates, Polygon.class, this::parsePolygon);
        return geometryFactory.createMultiPolygon(polygons);
    }

    @SuppressWarnings("unchecked")
    private <T extends Geometry> T[] parseGeometries(ArrayNode coordinates, Class<T> targetType,
            Function<ArrayNode, T> functor) {

        int size = coordinates.size();
        T[] geoms = (T[]) Array.newInstance(targetType, size);
        IntStream.range(0, size)
                .forEach((i) -> geoms[i] = functor.apply((ArrayNode) coordinates.get(i)));
        return geoms;
    }

    private Polygon parsePolygon(ArrayNode coordinates) {
        LinearRing shell = parseLinearRing((ArrayNode) coordinates.get(0));
        final LinearRing[] holes;
        if (coordinates.size() > 1) {
            holes = new LinearRing[coordinates.size() - 1];
            IntStream.range(1, coordinates.size()).forEach((i) -> {
                holes[i - 1] = parseLinearRing((ArrayNode) coordinates.get(i));
            });
        } else {
            holes = null;
        }
        return geometryFactory.createPolygon(shell, holes);
    }

    private LinearRing parseLinearRing(ArrayNode coordinates) {
        Coordinate[] coords = parseCoordinateArray(coordinates);
        return geometryFactory.createLinearRing(coords);
    }

    private LineString parseLineString(ArrayNode coordinates) {
        Coordinate[] coords = parseCoordinateArray(coordinates);
        return geometryFactory.createLineString(coords);
    }

    private MultiPoint parseMultiPoint(ArrayNode coordinates) {
        Coordinate[] coords = parseCoordinateArray(coordinates);
        return geometryFactory.createMultiPoint(coords);
    }

    private Coordinate[] parseCoordinateArray(ArrayNode coordinates) {
        final int size = coordinates.size();
        Coordinate[] coordArray = new Coordinate[size];
        IntStream.range(0, size)
                .forEach((i) -> coordArray[i] = parseCoordinate((ArrayNode) coordinates.get(i)));
        return coordArray;
    }

    private Point parsePoint(ArrayNode coordinates) {
        Coordinate coordinate = parseCoordinate(coordinates);
        return geometryFactory.createPoint(coordinate);
    }

    private Coordinate parseCoordinate(ArrayNode coordinate) {
        double x = coordinate.get(0).doubleValue();
        double y = coordinate.get(1).doubleValue();
        return new Coordinate(x, y);
    }

    private Coordinate parseCoordinate(ArrayNode coordinate, Coordinate target) {
        target.x = coordinate.get(0).doubleValue();
        target.y = coordinate.get(1).doubleValue();
        return target;
    }

    public boolean isGeometry(JsonNode value) {
        if (!(value instanceof ObjectNode)) {
            return false;
        }
        JsonNode typeNode = value.get("type");
        return typeNode instanceof TextNode && isGeometry(((TextNode) typeNode).asText());
    }

    private static final Set<String> geomTypes = new HashSet<>(Arrays.asList(//
            "Point", "MultiPoint", "LineString", "MultiLineString", "Polygon", "MultiPolygon",
            "GeometryCollection"));

    private boolean isGeometry(String type) {
        return geomTypes.contains(type);
    }

}
