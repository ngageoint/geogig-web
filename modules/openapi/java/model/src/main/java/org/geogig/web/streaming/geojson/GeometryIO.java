package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geogig.web.streaming.GeogigFeatureReader;
import org.geogig.web.streaming.json.ParserUtils;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeometryIO {

    private ObjectMapper mapper;

    public GeometryIO(String mediaType) {
        this.mapper = ParserUtils.getMapper(mediaType);
    }

    public Geometry readFrom(InputStream entityStream) throws IOException {
        JsonParser parser = mapper.getFactory().createParser(entityStream);
        ParserUtils.requireNext(parser, JsonToken.START_OBJECT);
        ObjectNode geometryNode = parser.readValueAsTree();
        GeometryReader reader = new GeometryReader(GeogigFeatureReader.DEFAULT_GEOMETRY_FACTORY);
        Geometry geom = reader.parse(geometryNode);
        return geom;
    }

    public void writeTo(Geometry geometry, OutputStream entityStream) throws IOException {
        JsonGenerator generator = mapper.getFactory().createGenerator(entityStream);
        GeometryWriter writer = new GeometryWriter(generator);
        writer.write(geometry);
        generator.flush();
    }

}
