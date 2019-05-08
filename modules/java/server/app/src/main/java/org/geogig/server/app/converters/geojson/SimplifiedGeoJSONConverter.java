package org.geogig.server.app.converters.geojson;

import java.io.IOException;
import java.io.OutputStream;

import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.streaming.json.ParserUtils;
import org.geotools.feature.FeatureCollection;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link HttpMessageConverter} that writes {@link FeatureCollection}s in JSON and SMILE (binary
 * JSON) formats.
 * <p>
 * TODO: control coordinate precision. User could supply a precision argument to limit the number of
 * decimals, possibly also some kind of 'auto' precision value to the server to generate a sensible
 * number of decimals based on the CRS units (e.g. 3 decimals for meters, 7 for degrees, etc). (Q:
 * should it default to 9 decimals to match geogig's coordinate comparison settings when computing a
 * geometry hash?)
 */
@Slf4j
public class SimplifiedGeoJSONConverter
        extends AbstractHttpMessageConverter<org.geogig.web.model.FeatureCollection> {

    private static final MediaType JSON_FEATURECOLLECTION = MediaType
            .valueOf(AppMediaTypes.SIMPLIFIED_GEOJSON);

    private static final MediaType BSON_FEATURECOLLECTION = MediaType
            .valueOf(AppMediaTypes.SIMPLIFIED_GEOJSON_BINARY);

    private final ObjectMapper jsonMapper;

    public SimplifiedGeoJSONConverter(MediaType mediaType, ObjectMapper objectMapper) {
        super(mediaType);
        this.jsonMapper = objectMapper;
    }

    protected @Override boolean supports(Class<?> clazz) {
        return GeogigFeatureCollection.class.isAssignableFrom(clazz);
    }

    public static SimplifiedGeoJSONConverter simplifiedGeoJSON() {
        return new SimplifiedGeoJSONConverter(JSON_FEATURECOLLECTION,
                ParserUtils.getMapper(AppMediaTypes.SIMPLIFIED_GEOJSON));
    }

    public static SimplifiedGeoJSONConverter simplifiedGeoJSONSmile() {
        return new SimplifiedGeoJSONConverter(BSON_FEATURECOLLECTION,
                ParserUtils.getMapper(AppMediaTypes.SIMPLIFIED_GEOJSON_BINARY));
    }

    //@formatter:off
    protected @Override org.geogig.web.model.FeatureCollection readInternal(
            Class<? extends org.geogig.web.model.FeatureCollection> clazz,
            HttpInputMessage inputMessage
            ) throws IOException, HttpMessageNotReadableException {
        //@formatter:on
        throw new UnsupportedOperationException();
    }

    //@formatter:off
    protected @Override void writeInternal(
            org.geogig.web.model.FeatureCollection t, 
            HttpOutputMessage outputMessage
            ) throws IOException, HttpMessageNotWritableException {
        //@formatter:on

        GeogigFeatureCollection collection = (GeogigFeatureCollection) t;
        final MediaType contentType = outputMessage.getHeaders().getContentType();
        final RevisionFeatureType featureType = t.getFeatureType();
        final String typeName = featureType == null ? "<unknown>" : featureType.getName();
        log.debug("Encoding FeatureCollection of {} as {}", typeName, contentType);

        OutputStream out = outputMessage.getBody();
        ObjectMapper mapper = this.jsonMapper;
        JsonGenerator generator = mapper.getFactory().createGenerator(out);
        generator.writeStartObject();
        generator.writeStringField("type", "FeatureCollection");
        generator.writeFieldName("type");
        mapper.writeValue(generator, collection.getFeatureType());
        generator.writeFieldName("features");
        generator.writeStartArray();
        writeFeatures(mapper, generator, collection);
        generator.writeEndArray();
        generator.writeEndObject();
        generator.flush();
    }

    private void writeFeatures(ObjectMapper mapper, JsonGenerator generator,
            GeogigFeatureCollection collection) throws IOException {

        RevisionFeatureType type = collection.getFeatureType();
        while (collection.hasNext()) {
            GeogigFeature next = collection.next();
            encode(mapper, generator, next, type);
        }
    }

    private void encode(ObjectMapper mapper, JsonGenerator gen, GeogigFeature feature,
            RevisionFeatureType type) throws IOException {

        gen.writeStartObject();
        gen.writeStringField("id", feature.getId());
        gen.writeFieldName("properties");
        gen.writeStartArray();
        for (SimplePropertyDescriptor prop : type.getProperties()) {
            String name = prop.getName();
            Object value = feature.get(name);
            writeValue(mapper, gen, value);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }

    private void writeValue(ObjectMapper mapper, JsonGenerator gen, Object value)
            throws JsonGenerationException, JsonMappingException, IOException {

        if (value instanceof Geometry) {
            writeGeometry(mapper, gen, (Geometry) value);
        } else {
            mapper.writeValue(gen, value);
        }
    }

    private void writeGeometry(ObjectMapper mapper, JsonGenerator gen, Geometry value)
            throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("dim", 2);// for now hardcoded as 2-dimensional
        gen.writeFieldName("coords");
        writeCoordinates(gen, value);
        gen.writeEndObject();
    }

    private void writeCoordinates(JsonGenerator gen, Geometry value) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.isEmpty()) {
            gen.writeStartArray();
            gen.writeEndArray();
            return;
        }
        if (value instanceof GeometryCollection) {
            gen.writeStartArray();
            for (int i = 0; i < value.getNumGeometries(); i++) {
                writeCoordinates(gen, value.getGeometryN(i));
            }
            gen.writeEndArray();
        } else if (value instanceof Polygon) {
            gen.writeStartArray();
            Polygon polygon = (Polygon) value;
            writeCoordinates(gen, polygon.getExteriorRing());
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                writeCoordinates(gen, polygon.getInteriorRingN(i));
            }
            gen.writeEndArray();
        } else {
            value.apply(new CoordSeqEncoder(gen));
        }
    }

    private static class CoordSeqEncoder implements CoordinateSequenceFilter {

        private JsonGenerator gen;

        CoordSeqEncoder(JsonGenerator gen) {
            this.gen = gen;
        }

        public @Override void filter(CoordinateSequence seq, int ignored) {
            final int dim = 2;
            int size = seq.size();
            try {
                gen.writeStartArray();
                for (int i = 0; i < size; i++) {
                    for (int d = 0; d < dim; d++) {
                        gen.writeNumber(seq.getOrdinate(i, d));
                    }
                }
                gen.writeEndArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //@formatter:off
        public boolean isDone() {return true;}//return true so filter() is called only once, for the first coordinate
        public @Override boolean isGeometryChanged() {return false;}
        //@formatter:on

    }
}
