package org.geogig.web.model;

import static org.geogig.web.model.ValueType.BIG_DECIMAL;
import static org.geogig.web.model.ValueType.BIG_INTEGER;
import static org.geogig.web.model.ValueType.BOOLEAN;
import static org.geogig.web.model.ValueType.BOOLEAN_ARRAY;
import static org.geogig.web.model.ValueType.BYTE;
import static org.geogig.web.model.ValueType.BYTE_ARRAY;
import static org.geogig.web.model.ValueType.CHAR;
import static org.geogig.web.model.ValueType.CHAR_ARRAY;
import static org.geogig.web.model.ValueType.DATE;
import static org.geogig.web.model.ValueType.DATETIME;
import static org.geogig.web.model.ValueType.DOUBLE;
import static org.geogig.web.model.ValueType.DOUBLE_ARRAY;
import static org.geogig.web.model.ValueType.ENVELOPE_2D;
import static org.geogig.web.model.ValueType.FEATURE;
import static org.geogig.web.model.ValueType.FLOAT;
import static org.geogig.web.model.ValueType.FLOAT_ARRAY;
import static org.geogig.web.model.ValueType.GEOMETRY;
import static org.geogig.web.model.ValueType.GEOMETRYCOLLECTION;
import static org.geogig.web.model.ValueType.INTEGER;
import static org.geogig.web.model.ValueType.INTEGER_ARRAY;
import static org.geogig.web.model.ValueType.LINESTRING;
import static org.geogig.web.model.ValueType.LONG;
import static org.geogig.web.model.ValueType.LONG_ARRAY;
import static org.geogig.web.model.ValueType.MAP;
import static org.geogig.web.model.ValueType.MULTILINESTRING;
import static org.geogig.web.model.ValueType.MULTIPOINT;
import static org.geogig.web.model.ValueType.MULTIPOLYGON;
import static org.geogig.web.model.ValueType.NULL;
import static org.geogig.web.model.ValueType.POINT;
import static org.geogig.web.model.ValueType.POLYGON;
import static org.geogig.web.model.ValueType.SHORT;
import static org.geogig.web.model.ValueType.SHORT_ARRAY;
import static org.geogig.web.model.ValueType.STRING;
import static org.geogig.web.model.ValueType.STRING_ARRAY;
import static org.geogig.web.model.ValueType.TIME;
import static org.geogig.web.model.ValueType.TIMESTAMP;
import static org.geogig.web.model.ValueType.UUID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import lombok.NonNull;

/**
 * Utility methods to work with {@link ValueType}
 *
 */
public class ValueTypes {

    private static final Map<ValueType, Class<?>> bindings = new EnumMap<>(ValueType.class);

    private static final Map<Class<?>, ValueType> valueTypes = new HashMap<>();

    private static final void bind(ValueType type, Class<?> binding) {
        bindings.put(type, binding);
        valueTypes.put(binding, type);
    }

    static {
        bind(BIG_DECIMAL, BigDecimal.class);
        bind(BIG_INTEGER, BigInteger.class);
        bind(BOOLEAN, Boolean.class);
        bind(BOOLEAN_ARRAY, boolean[].class);
        bind(BYTE, Byte.class);
        bind(BYTE_ARRAY, byte[].class);
        bind(CHAR, Character.class);
        bind(CHAR_ARRAY, char[].class);
        bind(DATE, Date.class);
        bind(DATETIME, java.util.Date.class);
        bind(DOUBLE, Double.class);
        bind(DOUBLE_ARRAY, double[].class);
        bind(ENVELOPE_2D, Envelope.class);
        bind(FLOAT, Float.class);
        bind(FLOAT_ARRAY, float[].class);
        bind(GEOMETRY, Geometry.class);
        bind(GEOMETRYCOLLECTION, GeometryCollection.class);
        bind(INTEGER, Integer.class);
        bind(INTEGER_ARRAY, int[].class);
        bind(LINESTRING, LineString.class);
        bind(LONG, Long.class);
        bind(LONG_ARRAY, long[].class);
        bind(MAP, Map.class);
        bind(MULTILINESTRING, MultiLineString.class);
        bind(MULTIPOINT, MultiPoint.class);
        bind(MULTIPOLYGON, MultiPolygon.class);
        bind(POINT, Point.class);
        bind(POLYGON, Polygon.class);
        bind(SHORT, Short.class);
        bind(SHORT_ARRAY, short[].class);
        bind(STRING, String.class);
        bind(STRING_ARRAY, String[].class);
        bind(TIME, Time.class);
        bind(TIMESTAMP, Timestamp.class);
        bind(UUID, UUID.class);
        bind(NULL, Void.class);
        bind(FEATURE, RevisionFeatureType.class);
    }

    public static Class<?> getJavaTypeBinding(@NonNull ValueType valueType) {
        if (NULL == valueType) {
            throw new IllegalArgumentException("NULL is not supported as a featuretype attribute");
        }
        Class<?> binding = bindings.get(valueType);
        if (null == binding) {
            throw new IllegalArgumentException(
                    "Uknown attribute type binding enum value: " + valueType);
        }
        return binding;
    }

    public static ValueType forBinding(@NonNull Class<?> binding) {
        ValueType valueType = valueTypes.get(binding);
        if (null == valueType) {
            throw new IllegalArgumentException(
                    "No ValueType is bound to class " + binding.getName());
        }
        return valueType;
    }

}
