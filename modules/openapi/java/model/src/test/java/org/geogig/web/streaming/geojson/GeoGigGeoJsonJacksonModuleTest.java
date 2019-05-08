package org.geogig.web.streaming.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.Feature;
import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.UpdateFeaturesRequest;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoGigGeoJsonJacksonModuleTest {

    private ObjectMapper mapper;

    private JsonFactory factory;

    public @Before void before() {
        mapper = GeoGigGeoJsonJacksonModule.getMapper(AppMediaTypes.GEOJSON);
        factory = mapper.getFactory();
    }

    public @Test void testFeature() throws JsonParseException, IOException {
        parseAndEncode(Feature.class);
    }

    public @Test void testFeatureCollection() throws JsonParseException, IOException {
        parseAndEncode(FeatureCollection.class);
    }

    public @Test void testUpdateFeaturesRequest() throws JsonParseException, IOException {
        parseAndEncode(UpdateFeaturesRequest.class);
    }

    private <T> T parse(Class<T> modelClass) throws IOException, JsonParseException {
        JsonParser parser = factory.createParser(getResource(modelClass));
        //T parsed = parser.readValueAs(modelClass);
        T parsed = parser.readValueAs(modelClass);
        //T parsed = mapper.reader().readValue(parser, modelClass);
        assertNotNull(parsed);
        return parsed;
    }

    private <T> T parse(String content, Class<T> modelClass)
            throws IOException, JsonParseException {
        JsonParser parser = factory.createParser(content);
        T parsed = mapper.reader().readValue(parser, modelClass);
        assertNotNull(parsed);
        return parsed;
    }

    private <T> String encode(T value)
            throws IOException, JsonGenerationException, JsonMappingException {
        StringWriter writer = new StringWriter();
        mapper.writer().writeValue(writer, value);
        String encoded = writer.toString();
        return encoded;
    }

    private <T> T parseAndEncode(Class<T> modelClass) throws IOException, JsonParseException {
        T parsed1 = parse(modelClass);
        assertNotNull(parsed1);
        String encoded = encode(parsed1);
        T parsed2 = parse(encoded, modelClass);
        assertEquals(parsed1, parsed2);
        return parsed1;
    }

    private InputStream getResource(Class<?> modelClass) {
        String name = modelClass.getSimpleName() + ".json";
        InputStream resource = modelClass.getResourceAsStream(name);
        assertNotNull("resource not found: " + name, resource);
        return resource;
    }

}
