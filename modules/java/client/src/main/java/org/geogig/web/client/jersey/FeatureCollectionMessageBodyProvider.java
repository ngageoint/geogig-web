package org.geogig.web.client.jersey;

import static org.geogig.web.model.AppMediaTypes.GEOJSON;
import static org.geogig.web.model.AppMediaTypes.GEOJSON_SMILE;
import static org.geogig.web.model.AppMediaTypes.SIMPLIFIED_GEOJSON;
import static org.geogig.web.model.AppMediaTypes.SIMPLIFIED_GEOJSON_BINARY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.streaming.geojson.GeoGigGeoJsonJacksonModule;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

import lombok.extern.slf4j.Slf4j;

@Provider
@Singleton
@Produces({ GEOJSON, GEOJSON_SMILE, SIMPLIFIED_GEOJSON, SIMPLIFIED_GEOJSON_BINARY })
@Consumes({ GEOJSON, GEOJSON_SMILE, SIMPLIFIED_GEOJSON, SIMPLIFIED_GEOJSON_BINARY })
public class FeatureCollectionMessageBodyProvider
        extends AbstractMessageReaderWriterProvider<org.geogig.web.model.FeatureCollection> {

    private static final Logger log = LoggerFactory.getLogger(FeatureCollectionMessageBodyProvider.class);
    
    //@formatter:off
    private static final Set<MediaType> MEDIATYPES = ImmutableSet.of(
            MediaType.valueOf(GEOJSON),
            MediaType.valueOf(GEOJSON_SMILE), 
            MediaType.valueOf(SIMPLIFIED_GEOJSON), 
            MediaType.valueOf(SIMPLIFIED_GEOJSON_BINARY)
            );
    //@formatter:on

    @Override
    //@formatter:off
    public boolean isReadable(
            Class<?> type, 
            Type genericType, 
            Annotation[] annotations,
            MediaType mediaType) {

        return MEDIATYPES.contains(mediaType) && org.geogig.web.model.FeatureCollection.class.isAssignableFrom(type);
    }

    @Override
    //@formatter:off
    public boolean isWriteable(
            Class<?> type, 
            Type genericType, 
            Annotation[] annotations,
            MediaType mediaType) {

        return MEDIATYPES.contains(mediaType) && GeogigFeatureCollection.class.isAssignableFrom(type);
    }

    
    //@formatter:off
    public @Override GeogigFeatureCollection readFrom(
            Class<org.geogig.web.model.FeatureCollection> type,
            Type genericType, 
            Annotation[] annotations, 
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, 
            InputStream entityStream)
            throws IOException, WebApplicationException {
        
        //@formatter:on
        //log.debug("Parsing GeogigFeatureCollection from stream in {} format", mediaType);
        ObjectMapper mapper = GeoGigGeoJsonJacksonModule.getMapper(mediaType.toString());
        JsonParser parser = mapper.getFactory().createParser(entityStream);
        FeatureCollection collection = parser
                .readValueAs(org.geogig.web.model.FeatureCollection.class);
        return (GeogigFeatureCollection) collection;
    }

    //@formatter:off
    public @Override void writeTo(
            org.geogig.web.model.FeatureCollection features, 
            Class<?> type, 
            Type genericType,
            Annotation[] annotations, 
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, 
            OutputStream entityStream)
            throws IOException, WebApplicationException {
        //@formatter:on

        log.debug("Writing GeogigFeatureCollection to stream in {} format", mediaType);
        ObjectMapper mapper = GeoGigGeoJsonJacksonModule.getMapper(mediaType.toString());
        mapper.writeValue(entityStream, features);
    }

}
