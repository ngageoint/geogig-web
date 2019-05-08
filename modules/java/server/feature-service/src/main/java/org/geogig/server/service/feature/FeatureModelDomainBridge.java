package org.geogig.server.service.feature;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.geogig.model.FieldType;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.model.impl.RevFeatureTypeBuilder;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Strings;

/**
 * Converts revision objects to and from geogig's {@link RevObject object model} to Swagger's
 *
 */
public class FeatureModelDomainBridge {

    public static @Nullable ObjectId toId(@Nullable String hexId) {
        return hexId == null ? null : ObjectId.valueOf(hexId);
    }

    public @Nullable static String toId(@Nullable ObjectId id) {
        return id == null ? null : id.toString();
    }

    public static @Nullable BoundingBox toBounds(
            @Nullable org.opengis.geometry.BoundingBox bounds) {
        if (null == bounds || bounds.isEmpty()) {
            return null;
        }
        BoundingBox bbox = new BoundingBox();
        bbox.add(bounds.getMinimum(0));
        bbox.add(bounds.getMinimum(1));
        bbox.add(bounds.getMaximum(0));
        bbox.add(bounds.getMaximum(1));
        return bbox;
    }

    public static @Nullable BoundingBox toBounds(@Nullable Envelope bounds) {
        if (null == bounds || bounds.isNull()) {
            return null;
        }
        BoundingBox bbox = new BoundingBox();
        bbox.add(bounds.getMinX());
        bbox.add(bounds.getMinY());
        bbox.add(bounds.getMaxX());
        bbox.add(bounds.getMaxY());
        return bbox;
    }

    public static @Nullable Envelope toBounds(@Nullable BoundingBox bounds) {
        if (null == bounds) {
            return null;
        }
        Envelope env = new Envelope(//
                bounds.get(0).doubleValue(), //
                bounds.get(2).doubleValue(), //
                bounds.get(1).doubleValue(), //
                bounds.get(3).doubleValue());
        return env;
    }

    public static RevFeatureType toFeatureType(RevisionFeatureType obj) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        builder.setName(obj.getName());
        // builder.setNamespaceURI(obj.getNamespace());

        List<SimplePropertyDescriptor> properties = obj.getProperties();
        for (SimplePropertyDescriptor pd : properties) {
            final String name = pd.getName();
            final ValueType binding = pd.getBinding();
            final Class<?> typeBinding = FieldType.valueOf(binding.ordinal()).getBinding();
            final @Nullable CoordinateReferenceSystem crs = GeoToolsDomainBridge.toCrs(pd.getCrs());
            if (crs == null) {
                builder.add(name, typeBinding);
            } else {
                builder.add(name, typeBinding, crs);
            }
        }
        String defaultGeometry = obj.getDefaultGeometry();
        if (!Strings.isNullOrEmpty(defaultGeometry)) {
            SimplePropertyDescriptor geomProp = obj.getProperties().stream()
                    .filter((p) -> defaultGeometry.equals(p.getName())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "default geometry attribute '" + defaultGeometry + "' does not exist"));
            builder.setCRS(GeoToolsDomainBridge.toCrs(geomProp.getCrs()));
            builder.setDefaultGeometry(geomProp.getName());
        }

        SimpleFeatureType featureType = builder.buildFeatureType();
        ObjectId id = toId(obj.getId());
        RevFeatureType type;
        type = RevFeatureTypeBuilder.build(featureType);

        if (id != null && !id.equals(type.getId())) {
            throw new IllegalArgumentException(String.format(
                    "Provided feature type hash (%s) does not match computed hash (%s)", id,
                    type.getId()));
        }
        return type;
    }

    public static RevisionFeatureType toFeatureType(RevFeatureType t) {
        SimpleFeatureType type = (SimpleFeatureType) t.type();
        RevisionFeatureType featureType = GeoToolsDomainBridge.toFeatureType(type);
        featureType.id(toId(t.getId()));
        return featureType;
    }

}
