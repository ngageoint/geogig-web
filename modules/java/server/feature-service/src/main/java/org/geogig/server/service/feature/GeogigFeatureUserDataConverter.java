package org.geogig.server.service.feature;

import java.util.List;

import org.geogig.web.model.GeogigFeature;
import org.locationtech.jts.geom.Geometry;

import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import lombok.NonNull;

/**
 * Converts a {@link Geometry#getUserData() geometry user data} given as a {@link GeogigFeature}
 * instance
 * 
 * @see IUserDataConverter
 */
class GeogigFeatureUserDataConverter implements IUserDataConverter {

    private final List<String> encodeableAtributes;

    public GeogigFeatureUserDataConverter(@NonNull List<String> encodeableAtributes) {
        this.encodeableAtributes = encodeableAtributes;
    }

    public @Override void addTags(Object userData, @NonNull MvtLayerProps layerProps,
            @NonNull Builder featureBuilder) {
        if (userData == null) {
            return;
        }
        final GeogigFeature feature = (GeogigFeature) userData;

        // Set the feature's meta-id property @id, since VT requires feature ids to be of type long,
        // and geogig feature ids are strings
        final String id = feature.getId();
        final int idIndex = layerProps.addValue(id);
        featureBuilder.addTags(layerProps.addKey("@id"));
        featureBuilder.addTags(idIndex);

        encodeableAtributes.forEach(att -> {
            final Object value = feature.get(att);
            if (value != null) {
                final int tagIndex = layerProps.addKey(att);
                final int valueIndex = layerProps.addValue(value);
                final boolean valueIsValid = valueIndex > -1;
                if (valueIsValid) {
                    featureBuilder.addTags(tagIndex);
                    featureBuilder.addTags(valueIndex);
                }
            }
        });
    }

}
