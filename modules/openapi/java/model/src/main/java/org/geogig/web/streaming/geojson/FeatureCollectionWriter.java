package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.locationtech.jts.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;

/**
 * Streaming encoder for {@link GeogigFeatureCollection}s that writes JSON and SMILE (binary JSON)
 * GeoJSON.
 * <p>
 * TODO: control coordinate precision. User could supply a precision argument to limit the number of
 * decimals, possibly also some kind of 'auto' precision value to the server to generate a sensible
 * number of decimals based on the CRS units (e.g. 3 decimals for meters, 7 for degrees, etc). (Q:
 * should it default to 9 decimals to match geogig's coordinate comparison settings when computing a
 * geometry hash?)
 */
public class FeatureCollectionWriter {
    private static final Logger log = LoggerFactory.getLogger(FeatureCollectionWriter.class);

    //@formatter:off
    public void write(
            GeogigFeatureCollection features, 
            JsonGenerator generator
            ) throws IOException{
        //@formatter:on
        final RevisionFeatureType type = features.getFeatureType();
        final ObjectCodec codec = generator.getCodec();

        generator.writeStartObject();// root object
        generator.writeStringField("type", "FeatureCollection");

        final String defaultGeometry;
        if (null == type) {
            defaultGeometry = null;
        } else {
            defaultGeometry = type.getDefaultGeometry();
            generator.writeFieldName("featureType");
            codec.writeValue(generator, type);
        }

        generator.writeFieldName("crs");
        codec.writeValue(generator, crs(features));

        Optional<Long> size = features.getSize();
        Optional<BoundingBox> bounds = features.getBounds();
        if (size.isPresent()) {
            generator.writeNumberField("size", size.get().longValue());
        }
        if (bounds.isPresent()) {
            generator.writeFieldName("bounds");
            codec.writeValue(generator, bounds.get());
        }

        generator.writeFieldName("features");
        generator.writeStartArray();

        FeatureWriter featureWriter = new FeatureWriter(generator, defaultGeometry);
        Stopwatch sw = new Stopwatch();
        sw.start();
        int count = 0;
        while (features.hasNext()) {
            GeogigFeature feature = features.next();
            try {
                featureWriter.write(feature);
            } catch (IOException e) {
                features.close();
                throw e;
            }
            count++;
        }
        sw.stop();
        String msg = String.format("Encoded %,d features of %s in %s", count,
                (type == null ? "<unspecified type>" : type.getName()), sw.getTimeString());
        log.debug(msg);
        generator.writeEndArray();

        generator.writeEndObject();// root object
        generator.flush();
        // Map<String, Object> m = toGeoJson(features);
        // writer.writeValue(out, m);
        // out.flush();
    }

    private Map<String, Object> crs(GeogigFeatureCollection ft) {

        final RevisionFeatureType type = ft.getFeatureType();
        final String defaultGeometry = type == null ? null : type.getDefaultGeometry();

        SimplePropertyDescriptor crsProp = defaultGeometry == null ? null
                : ft.getFeatureType().getProperties().stream()
                        .filter((p) -> p.getName().equals(defaultGeometry)).findFirst()
                        .orElse(null);
        if (crsProp == null) {
            return null;
        }
        SRS srs = crsProp.getCrs();
        String srsName = srs.getAuthorityCode();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "name");
        m.put("properties", Collections.singletonMap("name", srsName));
        return m;
    }

}
