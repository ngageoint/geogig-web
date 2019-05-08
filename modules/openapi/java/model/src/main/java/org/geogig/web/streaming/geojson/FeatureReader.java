package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.streaming.GeogigFeatureReader;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.NonNull;

public class FeatureReader implements GeogigFeatureReader {

    private static final Logger log = LoggerFactory.getLogger(FeatureReader.class);

    private final RevisionFeatureType type;

    private JsonParser parser;

    private GeometryReader geometryGeoJSON;

    private FeatureReader(/* nullable */RevisionFeatureType type, JsonParser parser,
            GeometryFactory geometryFactory) throws IOException {
        this.type = type;
        this.parser = parser;
        this.geometryGeoJSON = new GeometryReader(geometryFactory);
    }

    //@formatter:off
    public static FeatureReader create(
            /* nullable */ RevisionFeatureType type,
            @NonNull JsonParser parser) 
            throws IOException {
        //@formatter:on

        return create(type, parser, GeogigFeatureReader.DEFAULT_GEOMETRY_FACTORY);
    }

    //@formatter:off
    public static FeatureReader create(
            /* nullable */ RevisionFeatureType type,
            @NonNull JsonParser parser, 
            @NonNull GeometryFactory geometryFactory)
            throws IOException {
        //@formatter:on

        return new FeatureReader(type, parser, geometryFactory);
    }

    //@formatter:off
    public static FeatureReader createUnknownType(
            @NonNull JsonParser parser)
            throws IOException {
        //@formatter:on

        return createUnknownType(parser, GeogigFeatureReader.DEFAULT_GEOMETRY_FACTORY);
    }

    //@formatter:off
    public static FeatureReader createUnknownType(
            @NonNull JsonParser parser,
            @NonNull GeometryFactory geometryFactory) 
            throws IOException {
        //@formatter:on

        return new FeatureReader(null, parser, geometryFactory);
    }

    public @Override GeogigFeature read() throws IOException {
        // ParserUtils.require(parser, JsonToken.START_OBJECT);
        if (JsonToken.START_OBJECT.equals(parser.currentToken())) {
            parser.nextToken();
        }

        ObjectNode properties;
        Geometry geometry = null;
        String geometryName = this.type == null ? null : this.type.getDefaultGeometry();
        GeogigFeature feature = new GeogigFeature();

        JsonToken token = parser.currentToken();
        while (token != null && JsonToken.END_OBJECT != token) {
            if (JsonToken.FIELD_NAME.equals(token)) {
                JsonToken nextValue = parser.nextValue();
                final String fieldName = parser.currentName();
                switch (fieldName) {
                case "id":
                    String id = parser.getValueAsString();
                    feature.setId(id);
                    break;
                case "properties":
                    properties = parser.readValueAsTree();
                    Iterator<Entry<String, JsonNode>> fields = properties.fields();
                    while (fields.hasNext()) {
                        Entry<String, JsonNode> e = fields.next();
                        String key = e.getKey();
                        JsonNode valueNode = e.getValue();
                        Object value = parseValue(valueNode);
                        feature.put(key, value);
                    }
                    break;
                case "geometry":
                    geometry = parser.readValueAs(Geometry.class);
                    break;
                case GeogigFeature.GEOMETRY_NAME_PLACEHOLDER:
                    if (geometryName == null) {
                        geometryName = parser.getValueAsString();
                    }
                    break;
                case "type":
                    break;// ignore
                default:
                    log.debug("Unknown property in GeoJSON feature: {}", fieldName);
                }
            }
            token = parser.nextToken();
        }
        if (geometry != null || geometryName != null) {
            if (geometryName == null) {
                geometryName = GeogigFeature.UNKNOWN_DEFAULT_GEOMETRY_NAME_PLACEHOLDER;
            }
            feature.put(geometryName, geometry);
        }
        return feature;
    }

    private GeogigFeature parseFeature(ObjectNode tree) {
        JsonNode id = tree.findValue("id");
        final String fid = id == null ? null : id.asText();
        final GeogigFeature feature = new GeogigFeature(fid, null);

        JsonNode geometryNode = tree.get("geometry");

        if (geometryNode != null) {
            Geometry defaultGeometry = null;
            String defaultGeomProp = type == null ? null : type.getDefaultGeometry();
            defaultGeometry = geometryGeoJSON.parse((ObjectNode) geometryNode);
            if (defaultGeomProp == null) {
                JsonNode customNameProperty = geometryNode.findValue("name");// custom name property
                if (null != customNameProperty) {
                    defaultGeomProp = customNameProperty.asText();
                }
            }
            if (defaultGeomProp == null) {
                defaultGeomProp = GeogigFeature.UNKNOWN_DEFAULT_GEOMETRY_NAME_PLACEHOLDER;
            }
            feature.put(defaultGeomProp, defaultGeometry);
        }

        JsonNode properties = tree.findValue("properties");
        Iterator<Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> e = fields.next();
            String key = e.getKey();
            JsonNode valueNode = e.getValue();
            Object value = parseValue(valueNode);
            feature.put(key, value);
        }
        return feature;
    }

    private Object parseValue(JsonNode valueNode) {
        JsonNodeType nodeType = valueNode.getNodeType();
        switch (nodeType) {
        case NULL:
        case MISSING:
            return null;
        case BOOLEAN:
            return Boolean.valueOf(valueNode.asBoolean());
        case NUMBER:
            return ((NumericNode) valueNode).numberValue();
        case OBJECT:
            ObjectNode objectNode = ((ObjectNode) valueNode);
            if (geometryGeoJSON.isGeometry(objectNode)) {
                return geometryGeoJSON.parse(objectNode);
            }
            JsonNode typeNode = objectNode.get("type");
            if (typeNode instanceof TextNode && "Feature".equals(typeNode.asText())) {
                return parseFeature(objectNode);
            }
            Iterator<String> fieldNames = objectNode.fieldNames();
            Map<String, Object> mapValue = new HashMap<>();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = objectNode.get(fieldName);
                Object value = parseValue(fieldNode);
                mapValue.put(fieldName, value);
            }
            return mapValue;
        // throw new UnsupportedOperationException(
        // "Unable to parse non geometry ObjectNode: " + objectNode);
        case POJO:
            Object pojo = ((POJONode) valueNode).getPojo();
            return pojo;
        case STRING:
            return valueNode.asText();
        case ARRAY:
//            throw new UnsupportedOperationException(
//                    "Array properties not yet supported: " + valueNode.toString());
            return null;
        case BINARY:
            return ((BinaryNode) valueNode).binaryValue();
        default:
            throw new UnsupportedOperationException(
                    "Unknown or unhandled JsonNodeType: " + nodeType);
        }
    }

    private Object parseArray(SimplePropertyDescriptor p, ArrayNode arrayNode) {
        // TODO Auto-generated method stub
        return null;
    }

}
