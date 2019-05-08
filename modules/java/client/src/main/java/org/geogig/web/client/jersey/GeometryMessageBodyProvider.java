package org.geogig.web.client.jersey;

import static org.geogig.web.model.AppMediaTypes.GEOJSON;
import static org.geogig.web.model.AppMediaTypes.GEOJSON_SMILE;

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

import org.geogig.web.streaming.geojson.GeometryIO;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.locationtech.jts.geom.Geometry;

import com.google.common.collect.ImmutableSet;

@Provider
@Singleton
@Produces({ GEOJSON, GEOJSON_SMILE })
@Consumes({ GEOJSON, GEOJSON_SMILE })
public class GeometryMessageBodyProvider extends AbstractMessageReaderWriterProvider<Geometry> {

    //@formatter:off
    private static final Set<MediaType> MEDIATYPES = ImmutableSet.of(
            MediaType.valueOf(GEOJSON),
            MediaType.valueOf(GEOJSON_SMILE)
            );
    //@formatter:on

    @Override
    //@formatter:off
    public boolean isReadable(
            Class<?> type, 
            Type genericType, 
            Annotation[] annotations,
            MediaType mediaType) {

        return MEDIATYPES.contains(mediaType) && Geometry.class.isAssignableFrom(type);
    }

    @Override
    //@formatter:off
    public boolean isWriteable(
            Class<?> type, 
            Type genericType, 
            Annotation[] annotations,
            MediaType mediaType) {

        return MEDIATYPES.contains(mediaType) && Geometry.class.isAssignableFrom(type);
    }

    
    //@formatter:off
    public @Override Geometry readFrom(
            Class<Geometry> type,
            Type genericType, 
            Annotation[] annotations, 
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, 
            InputStream entityStream)
            throws IOException, WebApplicationException {
        
        //@formatter:on

        GeometryIO io = new GeometryIO(mediaType.toString());
        Geometry geom = io.readFrom(entityStream);
        return geom;
    }

    //@formatter:off
    public @Override void writeTo(
            Geometry geom, 
            Class<?> type, 
            Type genericType,
            Annotation[] annotations, 
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, 
            OutputStream entityStream)
            throws IOException, WebApplicationException {
        //@formatter:on

        GeometryIO io = new GeometryIO(mediaType.toString());
        io.writeTo(geom, entityStream);
    }

}
