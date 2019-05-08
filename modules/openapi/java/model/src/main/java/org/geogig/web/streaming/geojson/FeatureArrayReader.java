package org.geogig.web.streaming.geojson;

import static org.geogig.web.streaming.json.ParserUtils.require;

import java.io.IOException;
import java.util.Optional;

import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.streaming.GeogigFeatureReader;
import org.geogig.web.streaming.GeogigFeatureStreamReader;
import org.geogig.web.streaming.json.ParserUtils;
import org.locationtech.jts.geom.GeometryFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import lombok.NonNull;

class FeatureArrayReader implements GeogigFeatureStreamReader {

    private final RevisionFeatureType type;

    private JsonParser parser;

    private FeatureReader featureReader;

    public FeatureArrayReader(/* nullable */RevisionFeatureType type, @NonNull JsonParser parser)
            throws IOException {
        this.type = type;
        this.parser = parser;

        @NonNull
        GeometryFactory geometryFactory = GeogigFeatureReader.GEOM_FAC.get();
        this.featureReader = FeatureReader.create(type, parser, geometryFactory);
        require(parser, JsonToken.START_ARRAY);
    }

    public @Override void close() {
        GeogigFeatureReader.GEOM_FAC.remove();
        JsonParser p = this.parser;
        this.parser = null;
        if (p != null) {
            try {
                p.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public @Override Optional<GeogigFeature> tryNext() throws IOException {
        JsonToken nextValue = parser.nextValue();
        if (JsonToken.END_ARRAY.equals(nextValue)) {
            close();
            return Optional.empty();
        }
        ParserUtils.require(parser, JsonToken.START_OBJECT);
        GeogigFeature feature = (GeogigFeature) featureReader.read();
        return Optional.of(feature);
    }
}
