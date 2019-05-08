package org.geogig.web.streaming.geojson;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;

class FeatureCollectionReader {

    //@formatter:off
    public GeogigFeatureCollection read(
            JsonParser parser)
            throws IOException,IllegalArgumentException {
        
        //@formatter:on
        GeogigFeatureCollection collection;
        try {
            JsonToken currentToken = parser.currentToken();
            if (JsonToken.START_OBJECT == currentToken) {
                parser.nextFieldName();
                currentToken = parser.currentToken();
            }
            if (JsonToken.VALUE_STRING == currentToken) {
                String currentName = parser.currentName();
                if (!"type".equals(currentName)) {
                    throw new IllegalArgumentException(
                            "Should be positioned at 'type' field name: " + currentName);
                }
                String type = parser.getText();
                if (!"FeatureCollection".equals(type)) {
                    throw new IllegalArgumentException(
                            "Expected type=FeatureCollection, got " + type);
                }
                currentToken = parser.nextToken();
            }
            /* nullable */RevisionFeatureType featureType = null;
            /* nullable */TreeNode crs = null;
            /* nullable */Long size = null;
            /* nullable */BoundingBox bounds = null;

            while (null != currentToken && JsonToken.END_OBJECT != currentToken) {
                final String currentFieldName = parser.currentName();
                currentToken = parser.nextValue();
                FeatureArrayReader featuresReader;
                switch (currentFieldName) {
                case "type":
                    String type = parser.getText();
                    if (!"FeatureCollection".equals(type)) {
                        throw new IllegalArgumentException(
                                "Expected type=FeatureCollection, got " + type);
                    }
                    break;
                case "featureType":
                    featureType = parser.readValueAs(RevisionFeatureType.class);
                    Objects.requireNonNull(featureType);
                    break;
                case "crs":
                    crs = parser.readValueAsTree();
                    break;
                case "size":
                    size = parser.getLongValue();
                    break;
                case "bounds":
                    bounds = parser.readValueAs(BoundingBox.class);
                    break;
                case "features":
                    if (JsonToken.START_ARRAY == currentToken) {
                        featuresReader = new FeatureArrayReader(featureType, parser);
                        collection = GeogigFeatureCollection.streamingCollection(featureType,
                                featuresReader);
                        if (size != null) {
                            collection.setSize(size.longValue());
                        }
                        if (bounds != null) {
                            collection.setBounds(bounds);
                        }
                        return collection;
                    }
                default:
                    break;
                }
                currentToken = parser.nextToken();
            }
            collection = GeogigFeatureCollection.of(featureType, Collections.emptyIterator());
            return collection;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            closeQuiet(parser);
            throw ioe;
        } catch (Exception e) {
            closeQuiet(parser);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        // return collection;
    }

    private void closeQuiet(JsonParser parser) {
        try {
            parser.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
