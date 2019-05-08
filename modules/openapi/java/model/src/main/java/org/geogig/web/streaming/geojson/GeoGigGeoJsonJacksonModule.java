package org.geogig.web.streaming.geojson;

import static org.geogig.web.model.AppMediaTypes.GEOJSON;
import static org.geogig.web.model.AppMediaTypes.GEOJSON_SMILE;
import static org.geogig.web.model.AppMediaTypes.JSON;
import static org.geogig.web.model.AppMediaTypes.JSON_SMILE;

import java.io.IOException;

import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.streaming.GeogigFeatureReader;
import org.geogig.web.streaming.json.ParserUtils;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Registered under {@code META-INF/services/com.fasterxml.jackson.databind.Module}
 *
 */
public class GeoGigGeoJsonJacksonModule extends SimpleModule {
    
    private static final Logger log = LoggerFactory.getLogger(GeoGigGeoJsonJacksonModule.class);
    
    private static final long serialVersionUID = 1L;

    public static final GeoGigGeoJsonJacksonModule INSTANCE = new GeoGigGeoJsonJacksonModule();

    //@formatter:off
    public static final ObjectMapper JSON_MAPPER = newMapper(ParserUtils.JSON_FACTORY);
    //@formatter:on

    //@formatter:off
    public static final ObjectMapper SMILE_MAPPER = newMapper(ParserUtils.SMILE_FACTORY);
    //@formatter:on

    private static ObjectMapper newMapper(JsonFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.setDateFormat(new StdDateFormat());

        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(GeoGigGeoJsonJacksonModule.INSTANCE);
        return mapper;

    }

    public static ObjectMapper getMapper(String mediaType) {
        if (JSON.equals(mediaType) || GEOJSON.equals(mediaType)) {
            return JSON_MAPPER;
        } else if (JSON_SMILE.equals(mediaType) || GEOJSON_SMILE.equals(mediaType)) {
            return SMILE_MAPPER;
        }
        throw new IllegalArgumentException("Requested media type " + mediaType + " not supported");
    }

    public GeoGigGeoJsonJacksonModule() {
        super("GeoGigGeoJSONModule");

        addSerializer(new GeometrySerializer());
        addDeserializer(Geometry.class, new GeometryDeserializer());

        addSerializer(new FeatureSerializer());
        addDeserializer(org.geogig.web.model.Feature.class, new FeatureDeserializer());

        addSerializer(new FeatureCollectionSerializer());
        addDeserializer(org.geogig.web.model.FeatureCollection.class,
                new FeatureCollectionDeserializer());
    }

    public class GeometrySerializer extends StdSerializer<Geometry> {
        private static final long serialVersionUID = 1L;

        protected GeometrySerializer() {
            super(Geometry.class);
        }

        public @Override void serialize(Geometry value, JsonGenerator gen,
                SerializerProvider serializers) throws IOException {

            GeometryWriter writer = new GeometryWriter(gen);
            writer.write(value);
        }

    }

    public class GeometryDeserializer extends JsonDeserializer<Geometry> {

        public @Override Geometry deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {

            GeometryReader reader;
            reader = new GeometryReader(GeogigFeatureReader.DEFAULT_GEOMETRY_FACTORY);
            Geometry geom = reader.parse(p.readValueAsTree());
            return geom;
        }
    }

    public class FeatureSerializer extends StdSerializer<org.geogig.web.model.Feature> {

        private static final long serialVersionUID = 1L;

        protected FeatureSerializer() {
            super(org.geogig.web.model.Feature.class);
        }

        //@formatter:off
        public @Override void serialize(
                org.geogig.web.model.Feature value, 
                JsonGenerator gen,
                SerializerProvider serializers) 
                throws IOException {
            //@formatter:on

            if ("Feature".equals(value.getType())) {
                FeatureWriter writer = new FeatureWriter(gen, null);
                writer.write((GeogigFeature) value);
            } else if ("FeatureCollection".equals(value.getType())) {
                FeatureCollectionWriter writer = new FeatureCollectionWriter();
                writer.write((GeogigFeatureCollection) value, gen);
            } else {
                serializers.reportBadDefinition(value.getClass(), String.format(
                        "Type id handling not implemented for type %s (by serializer of type %s)",
                        value.getClass().getName(), getClass().getName()));
            }

        }

        //@formatter:off
        public @Override void serializeWithType(
                org.geogig.web.model.Feature value,
                JsonGenerator gen, 
                SerializerProvider serializers, 
                TypeSerializer typeSer)
                throws IOException {
          //@formatter:on
            serialize(value, gen, serializers);
        }

    }

    public class FeatureDeserializer extends JsonDeserializer<org.geogig.web.model.Feature> {

        //@formatter:off
        public @Override GeogigFeature deserialize(
                JsonParser p, 
                DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            //@formatter:on

            FeatureReader reader = FeatureReader.createUnknownType(p);
            try {
                GeogigFeature feature = reader.read();
                return feature;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        //@formatter:off
        public @Override org.geogig.web.model.Feature deserializeWithType(
                JsonParser p,
                DeserializationContext ctxt, 
                TypeDeserializer typeDeserializer) 
                        throws IOException {
            //@formatter:on

            final String typeIdFieldName = p.nextFieldName();
            if (!"type".equals(typeIdFieldName)) {
                throw new IllegalArgumentException(
                        "Expected 'type', got '" + typeIdFieldName + "'");
            }
            final String type = p.nextTextValue();
            if ("Feature".equalsIgnoreCase(type)) {
                FeatureReader reader = FeatureReader.createUnknownType(p);
                GeogigFeature feature = reader.read();
                return feature;
            } else if ("FeatureCollection".equalsIgnoreCase(type)) {
                FeatureCollectionReader reader = new FeatureCollectionReader();
                GeogigFeatureCollection collection = reader.read(p);
                return collection;
            }
            throw new UnsupportedOperationException("Unknown subtype of Feature: " + type);
        }
    }

    public class FeatureCollectionSerializer
            extends StdSerializer<org.geogig.web.model.FeatureCollection> {

        private static final long serialVersionUID = 1L;

        protected FeatureCollectionSerializer() {
            super(org.geogig.web.model.FeatureCollection.class);
        }

        //@formatter:off
        public @Override void serialize(
                org.geogig.web.model.FeatureCollection value, 
                JsonGenerator gen,
                SerializerProvider serializers) 
                throws IOException {
            //@formatter:on
            FeatureCollectionWriter writer = new FeatureCollectionWriter();
            try {
                writer.write((GeogigFeatureCollection) value, gen);
            } catch (IOException e) {
                log.debug("Feature stream write aborted by user");
                throw e;
            }
        }

        //@formatter:off
        public @Override void serializeWithType(
                org.geogig.web.model.FeatureCollection value,
                JsonGenerator gen, 
                SerializerProvider serializers, 
                TypeSerializer typeSer)
                throws IOException {
          //@formatter:on
            serialize(value, gen, serializers);
        }
    }

    public class FeatureCollectionDeserializer
            extends JsonDeserializer<org.geogig.web.model.FeatureCollection> {

        public @Override GeogigFeatureCollection deserialize(JsonParser p,
                DeserializationContext ctxt) throws IOException, JsonProcessingException {

            FeatureCollectionReader reader = new FeatureCollectionReader();
            GeogigFeatureCollection collection = reader.read(p);
            return collection;
        }

        //@formatter:off
        public @Override org.geogig.web.model.Feature deserializeWithType(
                JsonParser p,
                DeserializationContext ctxt, 
                TypeDeserializer typeDeserializer) 
                        throws IOException {
            //@formatter:on
            return deserialize(p, ctxt);
        }
    }
}
