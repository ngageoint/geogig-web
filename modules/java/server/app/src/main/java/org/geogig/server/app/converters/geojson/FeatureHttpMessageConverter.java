package org.geogig.server.app.converters.geojson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.Feature;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.streaming.geojson.GeoGigGeoJsonJacksonModule;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

/**
 */
public class FeatureHttpMessageConverter
        extends AbstractHttpMessageConverter<org.geogig.web.model.Feature> {

    private static final MediaType GEOJSON = MediaType.valueOf(AppMediaTypes.GEOJSON);

    private static final MediaType GEOJSON_SMILE = MediaType.valueOf(AppMediaTypes.GEOJSON_SMILE);

    private ObjectMapper mapper;

    private FeatureHttpMessageConverter(MediaType mediaType, ObjectMapper mapper) {
        super(mediaType);
        this.mapper = mapper;
        ;
    }

    public static FeatureHttpMessageConverter geoJSON() {
        return new FeatureHttpMessageConverter(GEOJSON, GeoGigGeoJsonJacksonModule.JSON_MAPPER);
    }

    public static FeatureHttpMessageConverter geoJSONSmile() {
        return new FeatureHttpMessageConverter(GEOJSON_SMILE,
                GeoGigGeoJsonJacksonModule.SMILE_MAPPER);
    }

    protected @Override boolean supports(Class<?> clazz) {
        return org.geogig.web.model.Feature.class.isAssignableFrom(clazz);
    }

    //@formatter:off
    protected @Override org.geogig.web.model.Feature readInternal(
            Class<? extends org.geogig.web.model.Feature> clazz, 
            HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        //@formatter:on

        InputStream entityStream = inputMessage.getBody();
        JsonParser parser = mapper.getFactory().createParser(entityStream);
        Feature feature = parser.readValueAs(Feature.class);

        return feature;
    }

    //@formatter:off
    @Override
    protected void writeInternal(
            org.geogig.web.model.Feature t, 
            HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        //@formatter:on

        Preconditions.checkArgument(t instanceof GeogigFeature);

        GeogigFeature feature = (GeogigFeature) t;
        OutputStream entityStream = outputMessage.getBody();
        ObjectWriter writer = mapper.writer();
        writer.writeValue(entityStream, feature);
    }

}
