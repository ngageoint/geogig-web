package org.geogig.web.model.geotools;

import static org.geogig.web.model.geotools.GeoToolsDomainBridge.toDiffFeatureType;
import static org.geogig.web.model.geotools.GeoToolsDomainBridge.toFeatureType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geogig.web.model.ObjectType;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DomainBridgeTest {
    private RevisionFeatureType poiRevType() {
        RevisionFeatureType t = new RevisionFeatureType();
        t.objectType(ObjectType.REVISIONFEATURETYPE);
        t.setName("Poi");
        t.addPropertiesItem(new SimplePropertyDescriptor().name("name").binding(ValueType.STRING)
                .nillable(true));
        t.addPropertiesItem(new SimplePropertyDescriptor().name("the_geom").binding(ValueType.POINT)
                .crs(new SRS().authorityCode("EPSG:4326")).nillable(true));
        t.setDefaultGeometry("the_geom");
        return t;
    }

    private SimpleFeatureType poiType() {
        try {
            return DataUtilities.createType("Poi", "name:String,the_geom:Point:srid=4326");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private SimpleFeature poi(int id) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(poiFeatureType);
        builder.set("name", String.valueOf(id));
        try {
            builder.set("the_geom", new WKTReader().read(String.format("POINT(%d %d)", id, id)));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return builder.buildFeature(String.valueOf(id));
    }

    private RevisionFeatureType poiRevType;

    private SimpleFeatureType poiFeatureType;

    public @Before void before() {
        this.poiRevType = poiRevType();
        this.poiFeatureType = poiType();
    }

    public @Test void testToSimpleFeatureType() {
        SimpleFeatureType actual = toFeatureType(poiRevType);
        assertEquals(poiFeatureType, actual);
    }

    public @Test void testToRevFeatureType() {
        RevisionFeatureType actual = toFeatureType(poiFeatureType);
        assertEquals(poiRevType, actual);
    }

    public @Test void testToDiffFeatureType() {
        RevisionFeatureType baseType = poiRevType();
        SimpleFeatureType diffFeatureType = toDiffFeatureType(baseType, poiFeatureType.getName());
        assertNotNull(diffFeatureType);

        List<String> attNames = diffFeatureType.getAttributeDescriptors().stream()
                .map(a -> a.getLocalName()).collect(Collectors.toList());
        assertEquals(attNames.toString(), 3, diffFeatureType.getAttributeCount());
        assertEquals(Lists.newArrayList("geogig.changeType", "old", "new"), attNames);
        AttributeDescriptor oldValDescriptor = diffFeatureType.getDescriptor("old");
        AttributeDescriptor newValDescriptor = diffFeatureType.getDescriptor("new");
        assertEquals(poiFeatureType, oldValDescriptor.getType());
        assertEquals(poiFeatureType, newValDescriptor.getType());
    }

    public @Test void testConvertDiffFeatureType() {
        RevisionFeatureType baseType = poiRevType();
        SimpleFeatureType diffFeatureType = toDiffFeatureType(baseType, poiFeatureType.getName());

        RevisionFeatureType revType = GeoToolsDomainBridge.toFeatureType(diffFeatureType);
        assertNotNull(revType);
        assertEquals(diffFeatureType.getTypeName(), revType.getName());
        assertNull(revType.getDefaultGeometry());
        List<SimplePropertyDescriptor> properties = revType.getProperties();
        List<String> attNames = diffFeatureType.getAttributeDescriptors().stream()
                .map(a -> a.getLocalName()).collect(Collectors.toList());
        assertEquals(attNames.toString(), 3, properties.size());
        assertEquals(Lists.newArrayList("geogig.changeType", "old", "new"), attNames);
        SimplePropertyDescriptor oldP = properties.get(1);
        SimplePropertyDescriptor newP = properties.get(2);

        assertEquals("old", oldP.getName());
        assertEquals("new", newP.getName());
        assertContentType(oldP, baseType);
        assertContentType(newP, baseType);

        SimpleFeatureType convertedBack = toFeatureType(revType);
        assertNotNull(convertedBack);
        assertDiffFeatureType(diffFeatureType, convertedBack);
    }

    private void assertDiffFeatureType(SimpleFeatureType expected, SimpleFeatureType actual) {

        List<AttributeDescriptor> attributeDescriptors = actual.getAttributeDescriptors();
        assertEquals(3, attributeDescriptors.size());
        assertDescriptor(expected.getAttributeDescriptors().get(0), attributeDescriptors.get(0));
        assertDescriptor(expected.getAttributeDescriptors().get(1), attributeDescriptors.get(1));
        assertDescriptor(expected.getAttributeDescriptors().get(2), attributeDescriptors.get(2));
    }

    private void assertDescriptor(AttributeDescriptor expected, AttributeDescriptor actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getMinOccurs(), actual.getMinOccurs());
        assertEquals(expected.getMaxOccurs(), actual.getMaxOccurs());
        assertType(expected.getType(), actual.getType());
    }

    private void assertType(AttributeType expected, AttributeType actual) {
        assertEquals(expected, actual);
    }

    private void assertContentType(SimplePropertyDescriptor prop, RevisionFeatureType baseType) {
        assertEquals(ValueType.FEATURE, prop.getBinding());
        assertNotNull(prop.getContentType());
        assertEquals(baseType, prop.getContentType());
    }
}
