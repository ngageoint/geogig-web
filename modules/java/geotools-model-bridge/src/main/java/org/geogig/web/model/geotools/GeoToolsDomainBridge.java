package org.geogig.web.model.geotools;

import static java.lang.Double.isFinite;

import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.ObjectType;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;
import org.geogig.web.model.ValueTypes;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.factory.Hints.ConfigurationMetadataKey;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.collection.AdaptorFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.ScreenMap;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeoToolsDomainBridge {

    static final FeatureTypeFactory FTF = CommonFactoryFinder.getFeatureTypeFactory(null);

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private static final String OLD_ATTRIBUTES_PREFIX = "old_";

    private static final String NEW_ATTRIBUTES_PREFIX = "new_";

    private static final String OLD_GEOM_ATTRIBUTE_USER_DATA_KEY = "geogig.diff.geometry.name.old";

    private static final String NEW_GEOM_ATTRIBUTE_USER_DATA_KEY = "geogig.diff.geometry.name.new";

    private static final String SCREENSISE_QUERY_HINT = "Renderer.ScreenSize";

    private static final String SCREENMAP_REPLACE_GEOMETRY_WITH_PX = "Renderer.ScreenMap.replaceGeometryWithPX";

    static Class<?> mapBinding(ValueType binding) {
        switch (binding) {
        case BIG_DECIMAL:
            return BigDecimal.class;
        case BIG_INTEGER:
            return BigInteger.class;
        case BOOLEAN:
            return Boolean.class;
        case BOOLEAN_ARRAY:
            return boolean[].class;
        case BYTE:
            return Byte.class;
        case BYTE_ARRAY:
            return byte[].class;
        case CHAR:
            return Character.class;
        case CHAR_ARRAY:
            return char[].class;
        case DATE:
            return Date.class;
        case DATETIME:
            return java.util.Date.class;
        case DOUBLE:
            return Double.class;
        case DOUBLE_ARRAY:
            return double[].class;
        case ENVELOPE_2D:
            return Envelope.class;
        case FLOAT:
            return Float.class;
        case FLOAT_ARRAY:
            return float[].class;
        case GEOMETRY:
            return Geometry.class;
        case GEOMETRYCOLLECTION:
            return GeometryCollection.class;
        case INTEGER:
            return Integer.class;
        case INTEGER_ARRAY:
            return int[].class;
        case LINESTRING:
            return LineString.class;
        case LONG:
            return Long.class;
        case LONG_ARRAY:
            return long[].class;
        case MAP:
            return Map.class;
        case MULTILINESTRING:
            return MultiLineString.class;
        case MULTIPOINT:
            return MultiPoint.class;
        case MULTIPOLYGON:
            return MultiPolygon.class;
        case POINT:
            return Point.class;
        case POLYGON:
            return Polygon.class;
        case SHORT:
            return Short.class;
        case SHORT_ARRAY:
            return short[].class;
        case STRING:
            return String.class;
        case STRING_ARRAY:
            return String[].class;
        case TIME:
            return Time.class;
        case TIMESTAMP:
            return Timestamp.class;
        case UUID:
            return UUID.class;
        case NULL:
            throw new IllegalArgumentException("NULL is not supported as a featuretype attribute");
        case FEATURE:
            return SimpleFeatureType.class;
        default:
            throw new IllegalArgumentException(
                    "Uknown attribute type binding enum value: " + binding);
        }
    }

    public static @NonNull BoundingBox toBounds(@Nullable Envelope bounds) {
        if (null == bounds || bounds.isNull()) {
            return new BoundingBox();
        }
        BoundingBox bbox = new BoundingBox();
        bbox.add(bounds.getMinX());
        bbox.add(bounds.getMinY());
        bbox.add(bounds.getMaxX());
        bbox.add(bounds.getMaxY());
        return bbox;
    }

    public static @NonNull ReferencedEnvelope toBounds(@Nullable BoundingBox bounds,
            @Nullable CoordinateReferenceSystem crs) {
        ReferencedEnvelope re = new ReferencedEnvelope(crs);
        Envelope env = toBounds(bounds);
        if (env != null) {
            re.init(env);
        }
        return re;
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

    public static SimpleFeatureType toFeatureType(RevisionFeatureType type) {
        return toFeatureType(type, new NameImpl(type.getName()));
    }

    public static Optional<String> getOldGeometryName(@NonNull SimpleFeatureType diffFeatureType) {
        Map<Object, Object> userData = diffFeatureType.getUserData();
        return Optional.ofNullable((String) userData.get(OLD_GEOM_ATTRIBUTE_USER_DATA_KEY));
    }

    public static Optional<String> getNewGeometryName(@NonNull SimpleFeatureType diffFeatureType) {
        Map<Object, Object> userData = diffFeatureType.getUserData();
        return Optional.ofNullable((String) userData.get(NEW_GEOM_ATTRIBUTE_USER_DATA_KEY));
    }

    public static SimpleFeatureType toDiffFeatureType(RevisionFeatureType baseType, Name typeName) {
        SimpleFeatureType commonType = toFeatureType(baseType, typeName);
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        FeatureTypeFactory typeFactory = builder.getFeatureTypeFactory();

        NameImpl oldName = new NameImpl("old");
        NameImpl newName = new NameImpl("new");

        AttributeDescriptor oldValDescriptor;
        AttributeDescriptor newValDescriptor;
        oldValDescriptor = typeFactory.createAttributeDescriptor(commonType, oldName, 1, 1, true,
                null);
        newValDescriptor = typeFactory.createAttributeDescriptor(commonType, newName, 1, 1, true,
                null);

        builder.add("geogig.changeType", Integer.class);
        builder.add(oldValDescriptor);
        builder.add(newValDescriptor);
        builder.setName(typeName);
        SimpleFeatureType diffFeatureType = builder.buildFeatureType();
        return diffFeatureType;
    }

    public static SimpleFeatureType toFlattenedDiffFeatureType(RevisionFeatureType baseType,
            Name typeName) {
        SimpleFeatureType commonType = toFeatureType(baseType, typeName);
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(typeName);
        builder.add("geogig.changeType", Integer.class);

        final String oldPrefix = OLD_ATTRIBUTES_PREFIX;
        final String newPrefix = NEW_ATTRIBUTES_PREFIX;
        String oldGeometryName = null, newGeometryName = null;

        List<AttributeDescriptor> atts = commonType.getAttributeDescriptors();
        for (AttributeDescriptor att : atts) {
            String name = att.getLocalName();
            Class<?> binding = att.getType().getBinding();
            if (att instanceof GeometryDescriptor) {
                CoordinateReferenceSystem crs = ((GeometryDescriptor) att)
                        .getCoordinateReferenceSystem();
                oldGeometryName = oldPrefix + name;
                newGeometryName = newPrefix + name;
                builder.add(oldGeometryName, binding, crs);
                builder.add(newGeometryName, binding, crs);
            } else {
                builder.add(oldPrefix + name, binding);
                builder.add(newPrefix + name, binding);
            }
        }

        SimpleFeatureType diffFeatureType = builder.buildFeatureType();
        Map<Object, Object> userData = diffFeatureType.getUserData();
        userData.put(OLD_GEOM_ATTRIBUTE_USER_DATA_KEY, oldGeometryName);
        userData.put(NEW_GEOM_ATTRIBUTE_USER_DATA_KEY, newGeometryName);
        return diffFeatureType;
    }

    public static SimpleFeatureType toFeatureType(RevisionFeatureType type, Name typeName) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder(GeoToolsDomainBridge.FTF);
        b.setName(typeName);

        b.setDefaultGeometry(type.getDefaultGeometry());

        for (SimplePropertyDescriptor p : type.getProperties()) {
            SRS srs = p.getCrs();
            String name = p.getName();
            ValueType binding = p.getBinding();
            Class<?> gtBinding = GeoToolsDomainBridge.mapBinding(binding);
            if (ValueType.FEATURE == binding) {
                RevisionFeatureType valueType = p.getContentType();
                Preconditions.checkNotNull(valueType);
                SimpleFeatureType valueFeatureType = toFeatureType(valueType);
                FeatureTypeFactory typeFactory = b.getFeatureTypeFactory();
                Name descriptorName = new NameImpl(name);
                AttributeDescriptor valueDescriptor = typeFactory.createAttributeDescriptor(
                        valueFeatureType, descriptorName, 1, 1, true, null);
                b.add(valueDescriptor);
            } else if (srs == null) {
                b.add(name, gtBinding);
            } else {
                if (srs.getAuthorityCode() != null) {
                    b.add(name, gtBinding, srs.getAuthorityCode());
                } else {
                    CoordinateReferenceSystem crs = null;
                    if (srs.getWkt() != null) {
                        try {
                            crs = CRS.parseWKT(srs.getWkt());
                        } catch (FactoryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    b.add(name, gtBinding, crs);
                }
            }
        }

        SimpleFeatureType featureType = b.buildFeatureType();
        return featureType;
    }

    public static RevisionFeatureType toFeatureType(SimpleFeatureType type) {

        RevisionFeatureType rtype = new RevisionFeatureType();
        rtype.objectType(ObjectType.REVISIONFEATURETYPE);
        rtype.name(type.getName().getLocalPart());
        // rtype.namespace(type.getName().getNamespaceURI());

        List<AttributeDescriptor> attributeDescriptors = type.getAttributeDescriptors();

        SimplePropertyDescriptor defaultGeometry = null;

        for (AttributeDescriptor att : attributeDescriptors) {
            SimplePropertyDescriptor pd = new SimplePropertyDescriptor();
            pd.name(att.getLocalName());
            // pd.namespace(pd.getNamespace());
            ValueType binding = getBinding(att.getType());
            pd.binding(binding);
            pd.nillable(att.isNillable());
            if (ValueType.FEATURE == binding) {
                SimpleFeatureType contentType = (SimpleFeatureType) att.getType();
                RevisionFeatureType nestedFeatureType = toFeatureType(contentType);
                pd.setContentType(nestedFeatureType);
            }

            // pd.minOccurs(att.getMinOccurs());
            // pd.maxOccurs(att.getMaxOccurs());
            if (att instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) att;
                pd.crs(toCrs(gd.getCoordinateReferenceSystem()));
            }
            rtype.addPropertiesItem(pd);

            if (att.equals(type.getGeometryDescriptor())) {
                defaultGeometry = pd;
            }
        }

        if (defaultGeometry != null) {
            rtype.defaultGeometry(defaultGeometry.getName());
        }
        return rtype;
    }

    public static ValueType getBinding(AttributeType type) {
        if (type instanceof SimpleFeatureType) {
            return ValueType.FEATURE;
        }
        Class<?> binding = type.getBinding();
        ValueType valueType = ValueTypes.forBinding(binding);
        return valueType;
    }

    public static @Nullable CoordinateReferenceSystem toCrs(@Nullable SRS srs) {
        if (srs == null) {
            return null;
        }
        final String authorityCode = srs.getAuthorityCode();
        final String wkt = srs.getWkt();
        CoordinateReferenceSystem crs;
        try {
            if (authorityCode != null) {
                crs = CRS.decode(authorityCode);
            } else if (wkt != null) {
                crs = CRS.parseWKT(wkt);
            } else {
                crs = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return crs;
    }

    public static @Nullable SRS toCrs(@Nullable CoordinateReferenceSystem crs) {
        if (crs == null) {
            return null;
        }
        String authorityCode = null;
        String wkt = null;
        try {
            authorityCode = CRS.lookupIdentifier(crs, true);
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        if (authorityCode == null) {
            wkt = crs.toString();
        }
        return new SRS().authorityCode(authorityCode).wkt(wkt);
    }

    public static FeatureQuery toQuery(Query query) {
        FeatureQuery featureQuery = new FeatureQuery();
        // null = all properties, empty = no properties, otherwise list as indicated
        String[] propertyNames = query.getPropertyNames();
        if (null != propertyNames) {
            // TODO: handle empty list = no properties
            featureQuery.setAttributes(Arrays.asList(propertyNames));
        }
        FeatureFilter filter = toFilter(query.getFilter());
        featureQuery.setFilter(filter);

        // offset and limit
        if (null != query.getStartIndex()) {
            Integer startIndex = query.getStartIndex();
            featureQuery.setOffset(startIndex);
        }
        if (!query.isMaxFeaturesUnlimited()) {
            int maxFeatures = query.getMaxFeatures();
            featureQuery.setLimit(maxFeatures);
        }

        final Hints hints = query.getHints();
        setScreenMap(featureQuery, hints);
        // simplification
        @Nullable
        Double simplificationDistance = (Double) hints.get(Hints.GEOMETRY_SIMPLIFICATION);
        featureQuery.setSimplificationDistance(simplificationDistance);

        // reprojection
        CoordinateReferenceSystem targetCrs = query.getCoordinateSystemReproject();
        if (targetCrs != null) {
            SRS crs = toCrs(targetCrs);
            if (crs != null) {
                featureQuery.setOutputCrs(crs);
            }
        }
        return featureQuery;
    }

    private static void setScreenMap(FeatureQuery featureQuery, Hints hints) {
        if (hints == null) {
            return;
        }
        // screenmap
        ScreenMap sm = (ScreenMap) hints.get(Hints.SCREENMAP);
        Rectangle screenSize = (Rectangle) hints
                .get(ConfigurationMetadataKey.get(SCREENSISE_QUERY_HINT));
        Integer screenWidth = null;
        Integer screenHeight = null;
        if (sm != null) {
            try {
                Field wf = sm.getClass().getDeclaredField("width");
                Field hf = sm.getClass().getDeclaredField("height");
                wf.setAccessible(true);
                hf.setAccessible(true);
                int width = (int) wf.get(sm);
                int height = (int) hf.get(sm);
                screenWidth = width;
                screenHeight = height;
            } catch (Exception e) {
                log.info("Unable to access ScreenMap's dimensions", e);
            }
        } else if (screenSize != null) {
            screenWidth = screenSize.width;
            screenHeight = screenSize.height;
        }
        featureQuery.setScreenWidth(screenWidth);
        featureQuery.setScreenHeight(screenHeight);
    }

    public static Query toQuery(SimpleFeatureType schema, FeatureQuery query) {
        Preconditions.checkNotNull(schema);
        Preconditions.checkNotNull(query);

        Query q = new Query(schema.getTypeName());

        CoordinateReferenceSystem targetCrs = null;
        SRS outputCrs = query.getOutputCrs();
        if (outputCrs != null) {
            targetCrs = GeoToolsDomainBridge.toCrs(outputCrs);
            q.setCoordinateSystemReproject(targetCrs);
        }

        FeatureFilter apiFilter = query.getFilter();
        Filter queryFilter = toFilter(schema, apiFilter, targetCrs);
        q.setFilter(queryFilter);
        List<String> attributes = query.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            q.setPropertyNames(attributes);
        }

        Integer offset = query.getOffset();
        Integer limit = query.getLimit();
        if (null != limit) {
            q.setMaxFeatures(limit.intValue());
        }
        if (null != offset) {
            q.setStartIndex(offset.intValue());
        }

        Integer screenWidth = query.getScreenWidth();
        Integer screenHeight = query.getScreenHeight();
        if (screenWidth != null && screenHeight != null && screenWidth > 0 && screenHeight > 0) {
            ScreenMap screenMap = new ScreenMap(0, 0, screenWidth, screenHeight);
            q.getHints().put(Hints.SCREENMAP, screenMap);
        }
        Double simplificationDistance = query.getSimplificationDistance();
        if (null != simplificationDistance) {
            q.getHints().put(Hints.GEOMETRY_SIMPLIFICATION, simplificationDistance);
        }
        if (query.getScreenmapReplaceGeom() != null) {
            ConfigurationMetadataKey key = Hints.ConfigurationMetadataKey
                    .get(SCREENMAP_REPLACE_GEOMETRY_WITH_PX);
            q.getHints().put(key, query
                    .getScreenmapReplaceGeom() == FeatureQuery.ScreenmapReplaceGeomEnum.WITHPX);
        }
        return q;
    }

    public static FeatureFilter toFilter(Filter gtFilter) {
        FeatureFilter filter = new FeatureFilter();

        if (!Filter.INCLUDE.equals(gtFilter)) {
            gtFilter = SimplifyingFilterVisitor.simplify(gtFilter);

            if (gtFilter instanceof Id) {
                Id fidFilter = (Id) gtFilter;
                Set<Identifier> identifiers = fidFilter.getIdentifiers();
                identifiers.forEach((i) -> filter.addFeatureIdsItem(i.toString()));
            } else if (gtFilter instanceof BBOX) {
                Envelope env = new Envelope();
                env = (Envelope) gtFilter.accept(ExtractBoundsFilterVisitor.BOUNDS_VISITOR, env);
                filter.setBbox(toBounds(env));
            } else {
                String cql = ECQL.toCQL(gtFilter);
                filter.setCqlFilter(cql);
            }
        }
        return filter;
    }

    public static Filter toFilter(SimpleFeatureType schema, FeatureFilter apiFilter) {
        return toFilter(schema, apiFilter, null);
    }

    public static Filter toFilter(SimpleFeatureType schema, FeatureFilter apiFilter,
            @Nullable CoordinateReferenceSystem targetCrs) {

        Filter filter = Filter.INCLUDE;
        if (apiFilter == null) {
            return filter;
        }

        final GeometryDescriptor defGeom = schema.getGeometryDescriptor();
        final String defaultGeometry = defGeom == null ? null : defGeom.getLocalName();
        final BoundingBox bbox = apiFilter.getBbox();
        final List<String> featureIds = apiFilter.getFeatureIds();
        final String cqlFilter = apiFilter.getCqlFilter();

        if (defGeom != null && bbox != null && !bbox.isEmpty()) {
            Preconditions.checkArgument(bbox.size() == 4);
            double minx = bbox.get(0);
            double miny = bbox.get(1);
            double maxx = bbox.get(2);
            double maxy = bbox.get(3);
            if (isFinite(miny) && isFinite(miny) && isFinite(maxy) && isFinite(maxy)) {
                CoordinateReferenceSystem filterCrs = targetCrs == null
                        ? defGeom.getCoordinateReferenceSystem()
                        : targetCrs;
                String srs = CRS.toSRS(filterCrs);
                filter = FF.bbox(defaultGeometry, minx, miny, maxx, maxy, srs);
            }
        } else if (featureIds != null && !featureIds.isEmpty()) {
            Set<FeatureId> fids;
            fids = featureIds.stream().map((id) -> FF.featureId(id)).collect(Collectors.toSet());
            filter = FF.id(fids);
        } else if (!Strings.isNullOrEmpty(cqlFilter)) {
            try {
                filter = ECQL.toFilter(cqlFilter);
            } catch (CQLException e) {
                throw new IllegalArgumentException(
                        String.format("Error parsing cql filter '%s'", cqlFilter), e);
            }
        }

        return filter;
    }

    public static SimpleFeature toFeature(SimpleFeatureType collectionSchema,
            GeogigFeature geogigFeature) {
        // TODO: optimize
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(collectionSchema);

        final String defaultSchemaGeomName;
        {
            GeometryDescriptor geometryDescriptor = collectionSchema.getGeometryDescriptor();
            if (geometryDescriptor == null) {
                defaultSchemaGeomName = null;
            } else {
                defaultSchemaGeomName = geometryDescriptor.getLocalName();
                final String defaultGeometryNameInFeature;
                if (geogigFeature.containsKey(defaultSchemaGeomName)) {
                    defaultGeometryNameInFeature = defaultSchemaGeomName;
                } else {
                    defaultGeometryNameInFeature = GeogigFeature.UNKNOWN_DEFAULT_GEOMETRY_NAME_PLACEHOLDER;
                }
                Object defaultGeometry = geogigFeature.get(defaultGeometryNameInFeature);
                builder.set(defaultSchemaGeomName, defaultGeometry);
            }
        }
        for (PropertyDescriptor att : collectionSchema.getDescriptors()) {
            Name name = att.getName();
            String localName = name.getLocalPart();
            if (!Objects.equals(defaultSchemaGeomName, localName)) {
                Object value = geogigFeature.get(localName);
                if (value instanceof GeogigFeature) {
                    SimpleFeatureType valueType = (SimpleFeatureType) att.getType();
                    GeogigFeature nested = (GeogigFeature) value;
                    value = toFeature(valueType, nested);
                }
                builder.set(localName, value);
            }
        }

        final String providedFid = geogigFeature.getId();
        SimpleFeature feature = builder.buildFeature(providedFid);
        if (providedFid != null) {
            feature.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
            feature.getUserData().put(Hints.PROVIDED_FID, providedFid);
        }
        return feature;
    }

    public static GeogigFeature toFeature(SimpleFeature feature) {
        RevisionFeatureType revtype = GeoToolsDomainBridge.toFeatureType(feature.getType());
        return toFeature(revtype, feature);
    }

    public static GeogigFeature toFeature(RevisionFeatureType revtype, SimpleFeature feature) {
        return toFeature(revtype, feature, null);
    }

    public static GeogigFeature toFeature(RevisionFeatureType revtype, SimpleFeature feature,
            @Nullable String removeTypeIdPrefix) {
        String fid = feature.getID();
        if (removeTypeIdPrefix != null) {
            fid = fid.substring(removeTypeIdPrefix.length());
        }
        GeogigFeature gf = new GeogigFeature(fid, revtype);
        for (SimplePropertyDescriptor p : revtype.getProperties()) {
            String name = p.getName();
            Object value = feature.getAttribute(name);
            if (value instanceof SimpleFeature) {
                SimpleFeature nested = (SimpleFeature) value;
                // TODO: optimize
                RevisionFeatureType nestedType = toFeatureType(nested.getType());
                value = toFeature(nestedType, nested);
            }
            gf.put(name, value);
        }
        return gf;
    }

    /**
     * 
     * @param collection the geogig collection to adapt as a geotools collection
     * @param fullSchema the full schema of the feature source. If the
     *        {@link GeogigFeatureCollection#getFeatureType() collection type} is null (may have
     *        been parsed from a geojson document that does not specify the featureType), the full
     *        schema is used.
     * @return
     */
    public static SimpleFeatureCollection toFeatureCollection(GeogigFeatureCollection collection,
            SimpleFeatureType fullSchema) {

        RevisionFeatureType type = collection.getFeatureType();
        SimpleFeatureType schema = type == null ? fullSchema
                : GeoToolsDomainBridge.toFeatureType(type);

        AdaptorFeatureCollection featureCollection = new AdaptorFeatureCollection(null, schema) {
            public @Override int size() {
                return 0;
            }

            protected @Override Iterator<SimpleFeature> openIterator() {
                Iterator<SimpleFeature> iterator;
                iterator = Iterators.transform(collection, (f) -> toFeature(schema, f));
                return iterator;
            }

            protected @Override void closeIterator(Iterator<SimpleFeature> close) {
                collection.close();
            }
        };
        return featureCollection;
    }

    public static GeogigFeatureCollection toFeatureCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> gtCollection) throws IOException {

        GeogigFeatureReaderGeotoolsAdapter readerAdapter;
        GeogigFeatureCollection geogigFeatures;

        readerAdapter = new GeogigFeatureReaderGeotoolsAdapter(gtCollection);
        RevisionFeatureType geogigFeatureType = readerAdapter.getType();
        geogigFeatures = GeogigFeatureCollection.streamingCollection(geogigFeatureType,
                readerAdapter);

        return geogigFeatures;
    }

}
