package org.geogig.web.streaming.json;

import static org.geogig.web.model.AppMediaTypes.GEOJSON;
import static org.geogig.web.model.AppMediaTypes.GEOJSON_SMILE;
import static org.geogig.web.model.AppMediaTypes.JSON;
import static org.geogig.web.model.AppMediaTypes.JSON_SMILE;
import static org.geogig.web.model.AppMediaTypes.SIMPLIFIED_GEOJSON;
import static org.geogig.web.model.AppMediaTypes.SIMPLIFIED_GEOJSON_BINARY;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ParserUtils {

    public static final JsonFactory JSON_FACTORY = new JsonFactory();

    public static final JsonFactory SMILE_FACTORY = new SmileFactory();

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(JSON_FACTORY)
            .registerModule(new JavaTimeModule());

    public static final ObjectMapper SMILE_MAPPER = new ObjectMapper(SMILE_FACTORY)
            .registerModule(new JavaTimeModule());

    public static ObjectMapper getMapper(String mediaType) {
        if (JSON.equals(mediaType) || GEOJSON.equals(mediaType)
                || SIMPLIFIED_GEOJSON.equals(mediaType)) {
            return JSON_MAPPER;
        } else if (JSON_SMILE.equals(mediaType) || GEOJSON_SMILE.equals(mediaType)
                || SIMPLIFIED_GEOJSON_BINARY.equals(mediaType)) {
            return SMILE_MAPPER;
        }
        throw new IllegalArgumentException("Requested media type " + mediaType + " not supported");
    }

    public static void require(JsonParser parser, JsonToken required) throws IOException {
        JsonToken token = parser.getCurrentToken();
        String currentName = parser.getCurrentName();
        Object currentValue = parser.getCurrentValue();
        if (!required.equals(token)) {
            throw new IllegalArgumentException(
                    String.format("expected current token %s, got %s", required, token));
        }
    }

    public static void requireNext(JsonParser parser, JsonToken required) throws IOException {
        JsonToken token = parser.nextToken();
        if (!required.equals(token)) {
            throw new IllegalArgumentException(
                    String.format("expected next token %s, got %s", required, token));
        }
    }

    public static void requireNextField(JsonParser parser, String fieldName,
            /* nullable */ Object expectedValue) throws IOException {

        requireNext(parser, JsonToken.FIELD_NAME);
        String actualFieldName = parser.getText();
        if (!fieldName.equals(actualFieldName)) {
            throw new IllegalArgumentException(
                    String.format("Expected fieldName %s, got %s", fieldName, actualFieldName));
        }
        if (expectedValue != null) {
            JsonToken valueToken = parser.nextValue();
            String textValue = parser.getText();
            if (!Objects.deepEquals(expectedValue, textValue)) {
                throw new IllegalArgumentException(
                        String.format("Expected fieldName %s=%s, but got %s=%s", fieldName,
                                expectedValue, fieldName, textValue));
            }
        }
    }

}
