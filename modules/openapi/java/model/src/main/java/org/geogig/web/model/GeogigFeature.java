package org.geogig.web.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class GeogigFeature extends org.geogig.web.model.Feature implements Map<String, Object> {

    public static final String GEOMETRY_NAME_PLACEHOLDER = "geometry_name";

    /**
     * Attribute name parsers and encoders shall use as the default geometry attribute name if such
     * information is not provided by the feature type or the feature type is not provided at all
     * when parsing/encoding a feature or feature collection
     */
    public static final String UNKNOWN_DEFAULT_GEOMETRY_NAME_PLACEHOLDER = "@geometry";

    private String fid;

    private Map<String, Object> delegate = new HashMap<>();

    /**
     * Creates a new feature with unknown type and no feature id
     */
    public GeogigFeature() {
        this.fid = null;
    }

    public GeogigFeature(String fid, RevisionFeatureType type) {
        this.fid = fid;
        this.setFeatureType(type);
    }

    /**
     * Copy constructor (note it doesn't create a deep clone)
     */
    public GeogigFeature(GeogigFeature f) {
        this.fid = f.getId();
        this.setFeatureType(f.getFeatureType());
        this.delegate.putAll(f);
    }

    public @Override String getType() {
        return "Feature";
    }

    public BoundingBox getBounds() {
        Envelope bounds = new Envelope();
        delegate.values().forEach((v) -> {
            if (v instanceof Geometry) {
                bounds.expandToInclude(((Geometry) v).getEnvelopeInternal());
            }
        });
        BoundingBox bb = new BoundingBox();
        bb.add(bounds.getMinX());
        bb.add(bounds.getMinY());
        bb.add(bounds.getMaxX());
        bb.add(bounds.getMaxY());
        return bb;
    }

    public String getId() {
        return fid;
    }

    public void setId(String id) {
        this.fid = id;
    }

    public @Override String toString() {
        RevisionFeatureType fetureType = getFeatureType();
        return String.format("[id=%s, featureType=%s, %s]", fid,
                fetureType == null ? null : fetureType.getName(), delegate.toString());
    }

    public @Override boolean equals(Object o) {
        if (o instanceof GeogigFeature) {
            GeogigFeature f = (GeogigFeature) o;
            return Objects.equals(fid, f.fid)
                    && Objects.equals(getFeatureType(), f.getFeatureType())
                    && Objects.equals(delegate, f.delegate);
        }
        return false;
    }

    public @Override int hashCode() {
        return Objects.hash(fid, getType(), getFeatureType(), delegate);
    }

    //@formatter:off
    public @Override int size() {return delegate.size();}
    public @Override boolean isEmpty() {return delegate.isEmpty();}
    public @Override boolean containsKey(Object key) {return delegate.containsKey(key);}
    public @Override boolean containsValue(Object value) {return delegate.containsValue(value);}
    public @Override Object get(Object key) {return delegate.get(key);}
    public @Override Object put(String key, Object value) {return delegate.put(key, value);}
    public @Override void putAll(Map<? extends String, ? extends Object> m) {delegate.putAll(m);}
    public @Override Object remove(Object key) {return delegate.remove(key);}
    public @Override void clear() {delegate.clear();}
    public @Override Set<String> keySet() {return delegate.keySet();}
    public @Override Collection<Object> values() {return delegate.values();}
    public @Override Set<Entry<String, Object>> entrySet() {return delegate.entrySet();}
    //@formatter:on
}
