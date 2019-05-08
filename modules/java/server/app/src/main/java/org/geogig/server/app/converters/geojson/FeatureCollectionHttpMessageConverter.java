package org.geogig.server.app.converters.geojson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.streaming.geojson.GeoGigGeoJsonJacksonModule;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

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
public class FeatureCollectionHttpMessageConverter
        extends AbstractHttpMessageConverter<org.geogig.web.model.FeatureCollection> {

    private static final MediaType GEOJSON = MediaType.valueOf(AppMediaTypes.GEOJSON);

    private static final MediaType GEOJSON_SMILE = MediaType.valueOf(AppMediaTypes.GEOJSON_SMILE);

    private ObjectMapper mapper;

    private FeatureCollectionHttpMessageConverter(MediaType mediaType, ObjectMapper mapper) {
        super(mediaType);
        this.mapper = mapper;
    }

    public static FeatureCollectionHttpMessageConverter geoJSON() {
        return new FeatureCollectionHttpMessageConverter(GEOJSON,
                GeoGigGeoJsonJacksonModule.JSON_MAPPER);
    }

    public static FeatureCollectionHttpMessageConverter geoJSONSmile() {
        return new FeatureCollectionHttpMessageConverter(GEOJSON_SMILE,
                GeoGigGeoJsonJacksonModule.SMILE_MAPPER);
    }

    protected @Override boolean supports(Class<?> clazz) {
        return org.geogig.web.model.FeatureCollection.class.isAssignableFrom(clazz);
    }

    //@formatter:off
    protected @Override org.geogig.web.model.FeatureCollection readInternal(
            Class<? extends org.geogig.web.model.FeatureCollection> clazz, 
            HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        //@formatter:on

        InputStream entityStream = inputMessage.getBody();
        JsonParser parser = mapper.getFactory().createParser(entityStream);
        FeatureCollection collection = parser.readValueAs(FeatureCollection.class);
        return collection;
    }

    //@formatter:off
    @Override
    protected void writeInternal(
            org.geogig.web.model.FeatureCollection t, 
            HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        //@formatter:on 

        final MediaType contentType = outputMessage.getHeaders().getContentType();
        final RevisionFeatureType featureType = t.getFeatureType();
        final String typeName = featureType == null ? "<unknown>" : featureType.getName();
        log.debug("Encoding FeatureCollection of {} as {}", typeName, contentType);

        Preconditions.checkArgument(t instanceof GeogigFeatureCollection);
        OutputStream entityStream = outputMessage.getBody();
        ObjectWriter writer = mapper.writer();
        writer.writeValue(entityStream, t);
    }

}
