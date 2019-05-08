package org.geogig.web.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;

import com.google.common.base.Preconditions;

public class SimpleTypeBuilder {

    private String name;

    private LinkedHashMap<String, SimplePropertyDescriptor> descriptors = new LinkedHashMap<>();

    private String defaultGeometryName;

    public SimpleTypeBuilder(String name) {
        this.name = name;
    }

    public SimpleTypeBuilder addProperty(String name, ValueType type, boolean nillable) {
        checkNotNull(name);
        checkNotNull(type);
        checkArgument(!isGeometry(type),
                "Illegal type: %s, use addGeometry(...) for geometric properties.", type);

        SimplePropertyDescriptor d = new SimplePropertyDescriptor().name(name).nillable(nillable)
                .binding(type);

        addDescriptor(d);
        return this;
    }

    public SimpleTypeBuilder addGeometry(String name, ValueType geometryType, boolean nillable,
            int epsgCode) {

        checkArgument(epsgCode > 0);
        String srs = "EPSG:" + epsgCode;
        return addGeometry(name, geometryType, nillable, new SRS().authorityCode(srs));
    }

    public SimpleTypeBuilder addGeometry(String name, ValueType geometryType, boolean nillable,
            String srsWkt) {

        checkNotNull(srsWkt);
        return addGeometry(name, geometryType, nillable, new SRS().wkt(srsWkt));
    }

    public SimpleTypeBuilder addGeometry(String name, ValueType geometryType, boolean nillable,
            SRS srs) {
        checkNotNull(name);
        checkNotNull(geometryType);
        checkNotNull(srs);
        checkArgument(srs.getAuthorityCode() != null || srs.getWkt() != null);
        checkArgument(isGeometry(geometryType),
                "Illegal type: %s, use addProperty(...) for non geometric properties.",
                geometryType);

        SimplePropertyDescriptor d = new SimplePropertyDescriptor().name(name).binding(geometryType)
                .nillable(nillable).crs(srs);

        addDescriptor(d);

        return this;
    }

    public void addDescriptor(SimplePropertyDescriptor d) {
        checkNotNull(d);
        checkNotNull(d.getName());
        checkNotNull(d.getBinding());
        if (isGeometry(d.getBinding())) {
            checkNotNull(d.getCrs());
        }
        descriptors.remove(d.getName());// to preserve ordering
        descriptors.put(d.getName(), d);
    }

    public SimpleTypeBuilder defaultGeometry(String propertyName) {
        this.defaultGeometryName = propertyName;
        return this;
    }

    public RevisionFeatureType build() {
        RevisionFeatureType type = new RevisionFeatureType();
        type.name(this.name);
        type.setProperties(new ArrayList<>(descriptors.values()));
        type.setDefaultGeometry(defaultGeometry());

        return type;
    }

    private @Nullable String defaultGeometry() {
        SimplePropertyDescriptor defGeom;
        if (this.defaultGeometryName == null) {
            defGeom = descriptors.values().stream().filter((p) -> isGeometry(p.getBinding()))
                    .findFirst().orElse(null);
        } else {
            defGeom = descriptors.get(defaultGeometryName);
            Preconditions.checkState(defGeom != null,
                    "Default geometry attribute %s does not exist", defaultGeometryName);
        }

        return defGeom == null ? null : defGeom.getName();
    }

    private boolean isGeometry(ValueType binding) {
        switch (binding) {
        case POINT:
        case MULTIPOINT:
        case LINESTRING:
        case MULTILINESTRING:
        case POLYGON:
        case MULTIPOLYGON:
        case GEOMETRY:
        case GEOMETRYCOLLECTION:
            return true;
        default:
            return false;
        }
    }
}
