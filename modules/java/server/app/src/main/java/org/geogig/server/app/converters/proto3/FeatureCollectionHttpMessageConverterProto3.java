package org.geogig.server.app.converters.proto3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.geogig.web.model.AppMediaTypes;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;
import org.geogig.web.streaming.GeogigFeatureStreamReader;
import org.locationtech.geogig.model.RevObjects;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

public class FeatureCollectionHttpMessageConverterProto3 implements HttpMessageConverter {

    private static final MediaType FORMATPROTOBUFF3 = MediaType
            .valueOf(AppMediaTypes.FormatProtoBufV1);

    public static FeatureCollectionHttpMessageConverterProto3 geoFormatProto3() {
        return new FeatureCollectionHttpMessageConverterProto3();
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return clazz.isAssignableFrom(org.geogig.web.model.FeatureCollection.class)
                && FORMATPROTOBUFF3.includes(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {

        return org.geogig.web.model.FeatureCollection.class.isAssignableFrom(clazz)
                && FORMATPROTOBUFF3.includes(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(FORMATPROTOBUFF3);

    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return readFeatureCollection(inputMessage.getBody());
    }

    public org.geogig.web.model.FeatureCollection readFeatureCollection(InputStream inputStream)
            throws IOException {
        Proto3FeatureReader featuresReader = new Proto3FeatureReader(inputStream);
        RevisionFeatureType featureType = featuresReader.getFeatureType();
        GeogigFeatureCollection collection = GeogigFeatureCollection.streamingCollection(null,
                featuresReader); // do not encode with FT
        return collection;
    }

    public FeatureCollectionProtos.FeatureType buildFeatureType(GeogigFeatureCollection fc) {
        RevisionFeatureType ft = fc.getFeatureType();
        FeatureCollectionProtos.FeatureType.Builder ftBuilder = FeatureCollectionProtos.FeatureType
                .newBuilder();

        ftBuilder.setFeatureTypeName(ft.getName());
        if (ft.getDefaultGeometry() != null) {
            ftBuilder.setDefaultGeometryName(ft.getDefaultGeometry());
        }

        List<FeatureCollectionProtos.FeatureType.AttributeType> atts = new ArrayList<>(
                ft.getProperties().size());
        for (SimplePropertyDescriptor prop : ft.getProperties()) {
            FeatureCollectionProtos.FeatureType.AttributeType.Builder attBuilder = FeatureCollectionProtos.FeatureType.AttributeType
                    .newBuilder();
            attBuilder.setName(prop.getName());
            attBuilder.setType(prop.getBinding().toString());
            if ((prop.getCrs() != null)) {
                String auth = prop.getCrs().getAuthorityCode();
                if (auth == null)
                    auth = RevObjects.NULL_CRS_IDENTIFIER;
                attBuilder.setSRS(auth);
            }
            ftBuilder.addAttribute(attBuilder.build());
        }

        return ftBuilder.build();
    }

    public FeatureCollectionProtos.Feature buildFeature(GeogigFeature f, int defaultGeomIdx,
            String[] propertyNames, WKBWriter wkbWriter) {
        FeatureCollectionProtos.Feature.Builder builder = FeatureCollectionProtos.Feature
                .newBuilder();
        builder.setID(f.getId());

        int idx = 0;
        for (String pname : propertyNames) {
            Object val = f.get(pname);

            if ( (val instanceof Geometry) && (idx == defaultGeomIdx) ){
                ByteString wkb = ByteString.copyFrom(wkbWriter.write(((Geometry) val)));
                builder.setGeom(wkb);
            } else {
                FeatureCollectionProtos.Value v = createValue(val, wkbWriter);
                builder.addValue(v);
            }
            idx++;
        }
        return builder.build();
    }

    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        outputMessage.getHeaders().setContentType(FORMATPROTOBUFF3);
        WKBWriter wkbWriter = new WKBWriter(); // make local

        GeogigFeatureCollection collection = (GeogigFeatureCollection) o;
        OutputStream out = outputMessage.getBody();

        if (isDiff(collection.getFeatureType())) {
            writeDiff(collection, out);
            return;
        }

        if (isConflict(collection.getFeatureType())) {
            writeConflict(collection, out);
            return;
        }

        FeatureCollectionProtos.FeatureType ft = buildFeatureType(collection);

        ft.writeDelimitedTo(out);
        int defaultGeomIdx = findDefaultGeomIndex(collection.getFeatureType());
        String[] propertyNames = collection.getFeatureType().getProperties().stream()
                .map(x -> x.getName()).toArray(String[]::new);

        while (collection.hasNext()) {
            GeogigFeature f = collection.next();
            FeatureCollectionProtos.Feature f2 = buildFeature(f, defaultGeomIdx, propertyNames,
                    wkbWriter);
            f2.writeDelimitedTo(out);
        }
    }

    // encode the collection as a conflict
    private void writeConflict(GeogigFeatureCollection collection, OutputStream out)
            throws IOException {
        WKBWriter wkbWriter = new WKBWriter(); // make local

        FeatureCollectionProtos.FeatureType ft = buildFeatureTypeConflict(collection.getFeatureType());
        ft.writeDelimitedTo(out);

        while (collection.hasNext()) {
            GeogigFeature f = collection.next();
            FeatureCollectionProtos.Feature f2 = buildFeatureConflict(f, ft, wkbWriter);
            f2.writeDelimitedTo(out);
        }
    }



    // encode the collection as a DIFF
    private void writeDiff(GeogigFeatureCollection collection, OutputStream out)
            throws IOException {
        WKBWriter wkbWriter = new WKBWriter(); // make local

        FeatureCollectionProtos.FeatureType ft = buildFeatureTypeDiff(collection.getFeatureType());
        ft.writeDelimitedTo(out);

        while (collection.hasNext()) {
            GeogigFeature f = collection.next();
            FeatureCollectionProtos.Feature f2 = buildFeatureDiff(f, ft, wkbWriter);
            f2.writeDelimitedTo(out);
        }
    }

    private FeatureCollectionProtos.Feature buildFeatureDiff(GeogigFeature f,
            FeatureCollectionProtos.FeatureType ft, WKBWriter wkbWriter) {
        FeatureCollectionProtos.Feature.Builder builder = FeatureCollectionProtos.Feature
                .newBuilder();
        builder.setID(f.getId());

        for (FeatureCollectionProtos.FeatureType.AttributeType att : ft.getAttributeList()) {
            if (att.getName().equals("geogig.changeType")) {
                builder.addValue(createValue(f.get("geogig.changeType"), wkbWriter));
            } else {
                String prefix = att.getName().split("\\.")[0];
                String pname = att.getName().split("\\.")[1];
                GeogigFeature ff = (GeogigFeature) f.get(prefix);
                Object value = null;
                if (ff != null)
                    value = ff.get(pname);
                builder.addValue(createValue(value, wkbWriter));
            }
        }
        return builder.build();
    }

    //this is the same format as diff, except there is a "geogig.conflictType" (int) field
    // this says which of the sub-features (ancestor, our, theirs) are NULL (i.e. deleted)
    //   first bit is ancestor ( & 0x01)
    //   second bit is ours ( & 0x02)
    //   third bit is theirs ( & 0x04)
    private FeatureCollectionProtos.Feature buildFeatureConflict(GeogigFeature f,
            FeatureCollectionProtos.FeatureType ft, WKBWriter wkbWriter) {
        FeatureCollectionProtos.Feature.Builder builder = FeatureCollectionProtos.Feature
                .newBuilder();
        builder.setID(f.getId());

        int code = 0;
        if (f.get("ancestor") != null)
            code += 1;
        if (f.get("ours") != null)
            code += 2;
        if (f.get("theirs") != null)
            code += 4;


        for (FeatureCollectionProtos.FeatureType.AttributeType att : ft.getAttributeList()) {
            if (att.getName().equals("geogig.conflictType")) {
                builder.addValue(createValue(code, wkbWriter));
            } else {
                String prefix = att.getName().split("\\.")[0];
                String pname = att.getName().split("\\.")[1];
                GeogigFeature ff = (GeogigFeature) f.get(prefix);
                Object value = null;
                if (ff != null)
                    value = ff.get(pname);
                builder.addValue(createValue(value, wkbWriter));
            }
        }
        return builder.build();
    }


    private FeatureCollectionProtos.Value createValue(Object val, WKBWriter wkbWriter) {
        FeatureCollectionProtos.Value.Builder value_builder = FeatureCollectionProtos.Value
                .newBuilder();

        if (val instanceof Geometry) {
            ByteString wkb = ByteString.copyFrom(wkbWriter.write(((Geometry) val)));
            value_builder.setGeomValue(wkb);
        } else {

            if (val == null)
                value_builder.setNullValue(true);
            else if (val instanceof String)
                value_builder.setStringValue((String) val);
            else if (val instanceof Float)
                value_builder.setFloatValue((Float) val);
            else if (val instanceof Double)
                value_builder.setDoubleValue((Double) val);
            else if (val instanceof Integer)
                value_builder.setIntValue((Integer) val);
            else if (val instanceof Long)
                value_builder.setLongValue((Long) val);
            else if (val instanceof Boolean)
                value_builder.setBoolValue((Boolean) val);
            else if (val instanceof Byte)
                value_builder.setByteValue(((Byte) val).intValue());
            else if (val instanceof Short)
                value_builder.setShortValue(((Short) val).intValue());
            else if (val instanceof Character)
                value_builder.setCharValue(val.toString());
            else if (val instanceof UUID)
                value_builder.setUuidValue(  val.toString());
            else if (val instanceof java.sql.Date)
                value_builder.setDateValue( ((java.sql.Date) val).getTime());
            else if (val instanceof java.sql.Time)
                value_builder.setTimeValue(((java.sql.Time) val).getTime());
            else if (val instanceof java.sql.Timestamp) {
                com.google.protobuf.Timestamp.Builder timestamp_builder =
                        com.google.protobuf.Timestamp.newBuilder();
                Instant t= ((java.sql.Timestamp)val).toInstant();
                timestamp_builder.setSeconds(t.getEpochSecond());
                timestamp_builder.setNanos(t.getNano());
                value_builder.setTimestampValue(timestamp_builder.build());
            }
            else if (val instanceof java.util.Date)
                value_builder.setDatetimeValue( ((java.util.Date) val).getTime());
        }
        return value_builder.build();
    }

    // create a feature type for a diff
    // FT looks like:
    // int geogig.changeType
    // object old.<property 1 Name>
    // object new.<property 1 Name>
    // ...
    // object old.<property 2 Name>
    // object new.<property 2 Name>
    // ...
    //
    // note - defaultGeometryName will be the name of the geometry column (not prefixed by new. or
    // old.)
    // Feature.geom will be empty (geometry will be in old.<geom> and new.<geom>
    // Feautre.ID will be the features ID (old and new will have the same ID)
    private FeatureCollectionProtos.FeatureType buildFeatureTypeDiff(RevisionFeatureType ft) {
        FeatureCollectionProtos.FeatureType.Builder ftBuilder = FeatureCollectionProtos.FeatureType
                .newBuilder();

        ftBuilder.setFeatureTypeName(ft.getName());

        SimplePropertyDescriptor old_desc = findAttribute(ft, "old");
        String defaultGeomName = old_desc.getContentType().getDefaultGeometry();
        ftBuilder.setDefaultGeometryName(defaultGeomName);

        List<FeatureCollectionProtos.FeatureType.AttributeType> atts = new ArrayList<>();

        // geogig.changeType
        ftBuilder.addAttribute(createAttribute("", findAttribute(ft, "geogig.changeType")));

        // actual feature types (old. and new.)
        for (SimplePropertyDescriptor prop : old_desc.getContentType().getProperties()) {
            FeatureCollectionProtos.FeatureType.AttributeType att = createAttribute("old.", prop);
            ftBuilder.addAttribute(att);
            att = createAttribute("new.", prop);
            ftBuilder.addAttribute(att);
        }

        return ftBuilder.build();
    }

    private FeatureCollectionProtos.FeatureType buildFeatureTypeConflict(RevisionFeatureType ft) {
        FeatureCollectionProtos.FeatureType.Builder ftBuilder = FeatureCollectionProtos.FeatureType
                .newBuilder();

        ftBuilder.setFeatureTypeName(ft.getName());

        SimplePropertyDescriptor old_desc = findAttribute(ft, "ancestor");
        String defaultGeomName = old_desc.getContentType().getDefaultGeometry();
        ftBuilder.setDefaultGeometryName(defaultGeomName);

        List<FeatureCollectionProtos.FeatureType.AttributeType> atts = new ArrayList<>();

        // "geogig.conflictType" (see buildFeatureConflict)
        SimplePropertyDescriptor spd = new SimplePropertyDescriptor()
                                            .name("geogig.conflictType")
                                            .nillable(false)
                                            .binding(ValueType.INTEGER);
        ftBuilder.addAttribute(createAttribute("", spd));

        // actual feature types (old. and new.)
        for (SimplePropertyDescriptor prop : old_desc.getContentType().getProperties()) {
            FeatureCollectionProtos.FeatureType.AttributeType att = createAttribute("ancestor.", prop);
            ftBuilder.addAttribute(att);
            att = createAttribute("theirs.", prop);
            ftBuilder.addAttribute(att);
            att = createAttribute("ours.", prop);
            ftBuilder.addAttribute(att);
        }

        return ftBuilder.build();
    }


    public FeatureCollectionProtos.FeatureType.AttributeType createAttribute(String prefix,
            SimplePropertyDescriptor prop) {
        FeatureCollectionProtos.FeatureType.AttributeType.Builder attBuilder = FeatureCollectionProtos.FeatureType.AttributeType
                .newBuilder();

        attBuilder.setName(prefix + prop.getName());
        attBuilder.setType(prop.getBinding().toString());
        if ((prop.getCrs() != null)) {
            String auth = prop.getCrs().getAuthorityCode();
            if (auth == null)
                auth = RevObjects.NULL_CRS_IDENTIFIER;
            attBuilder.setSRS(auth);
        }
        return attBuilder.build();
    }

    // returns true if this is a diff
    // diff -> has a property called "old" that is of type "FEATURE"
    private boolean isDiff(RevisionFeatureType featureType) {
        List<SimplePropertyDescriptor> props = featureType.getProperties();
        if (props.size() < 3)
            return false;
        SimplePropertyDescriptor old = findAttribute(featureType, "old");
        if (old == null)
            return false;
        if (old.getBinding().name().equals("FEATURE"))
            return true;
        return false;
    }

    // returns true if this is a Conflict
    // conflict -> has a property called "ancestor" that is of type "FEATURE"
    private boolean isConflict(RevisionFeatureType featureType) {
        List<SimplePropertyDescriptor> props = featureType.getProperties();
        if (props.size() < 3)
            return false;
        SimplePropertyDescriptor ancestor = findAttribute(featureType, "ancestor");
        if (ancestor == null)
            return false;
        if (ancestor.getBinding().name().equals("FEATURE"))
            return true;
        return false;
    }


    public SimplePropertyDescriptor findAttribute(RevisionFeatureType featureType, String name) {
        for (SimplePropertyDescriptor prop : featureType.getProperties()) {
            if (prop.getName().equals(name))
                return prop;
        }
        return null;
    }

    public int findDefaultGeomIndex(RevisionFeatureType ft) {
        String default_geom = ft.getDefaultGeometry();
        int idx = 0;
        for (SimplePropertyDescriptor prop : ft.getProperties()) {
            if (prop.getName().equals(default_geom))
                return idx;
            idx++;
        }
        return -1; // no geom
    }

    // ------------------------------------------------

    // feature reader for proto3
    // FORMAT:
    // <feature type size -- varint>
    // <FeatureType>
    // <feature 1 size varint>
    // <Feature 1>
    // <feature 2 size varint>
    // <Feature 2>
    // ..
    public static class Proto3FeatureReader implements GeogigFeatureStreamReader {

        InputStream stream;

        RevisionFeatureType featureType;

        WKBReader wkbReader;

        public Proto3FeatureReader(InputStream stream) throws IOException {
            this.stream = stream;
            this.featureType = readFeatureType(this.stream);
            this.wkbReader = new WKBReader();
        }

        RevisionFeatureType getFeatureType() {
            return this.featureType;
        }

        @Override
        public void close() {
            // framework should close stream (??)
        }

        // read a feature (for wrapper)
        // return Optional.empty if there's no more features to read
        @Override
        public Optional<GeogigFeature> tryNext() throws IOException {
            try {
                GeogigFeature f = readFeature();
                return Optional.ofNullable(f);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        // parse feature type
        // we use it to convert the attributes and put them in name->object map (in feature)
        // note - in general, we don't have (from QGIS);
        // * geometry column name
        // * geometry column specific type
        // * geometry column SRS
        // * feature type name (sometimes)
        public RevisionFeatureType readFeatureType(InputStream stream) throws IOException {
            FeatureCollectionProtos.FeatureType ft = FeatureCollectionProtos.FeatureType
                    .parseDelimitedFrom(stream);
            RevisionFeatureType result = new RevisionFeatureType();
            result.setName(ft.getFeatureTypeName());
            result.setDefaultGeometry(ft.getDefaultGeometryName());
            List<SimplePropertyDescriptor> properties = new ArrayList<>();
            for (FeatureCollectionProtos.FeatureType.AttributeType at : ft.getAttributeList()) {
                SimplePropertyDescriptor p = new SimplePropertyDescriptor();
                p.setName(at.getName());
                p.setNillable(Boolean.TRUE);
                p.setBinding(ValueType.fromValue(at.getType()));
                if ((at.getSRS() != null) && (!at.getSRS().equals(""))) {
                    SRS srs = new SRS();
                    srs.setAuthorityCode(at.getSRS());
                    p.setCrs(srs);
                }
                properties.add(p);
            }
            result.setProperties(properties);
            return result;
        }

        // read a feature from the stream
        public GeogigFeature readFeature() throws Exception {

            FeatureCollectionProtos.Feature f = FeatureCollectionProtos.Feature
                    .parseDelimitedFrom(stream);
            if (f == null) // EOF
                return null;
            Geometry geom = wkbReader.read(f.getGeom().toByteArray());
            String id = f.getID();
            if ((id != null) && (id.equals("")))
                id = null;
            GeogigFeature result = new GeogigFeature(id, featureType); // ID
            result.put(featureType.getDefaultGeometry(), geom); // GEOM

            ArrayList<FeatureCollectionProtos.Value> values = Lists.newArrayList(f.getValueList());
            for (int idx = 0; idx < featureType.getProperties().size(); idx++) {
                SimplePropertyDescriptor propertyDescriptor = featureType.getProperties().get(idx);
                if (propertyDescriptor.getName().equals(featureType.getDefaultGeometry()))
                    continue; // already handled, above
                FeatureCollectionProtos.Value value = values.get(idx);
                Object v = convert(propertyDescriptor, value);
                result.put(propertyDescriptor.getName(), v);
            }
            return result;
        }

        // convert a proto3 attribute to actual value
        public Object convert(SimplePropertyDescriptor propertyDescriptor,
                FeatureCollectionProtos.Value value) throws Exception {
            int fieldNo = value.getValueTypeCase().getNumber();
            if (fieldNo == 7)
                return null;
            else if (fieldNo == 1)
                return value.getStringValue();
            else if (fieldNo == 2)
                return value.getDoubleValue();
            else if (fieldNo == 3)
                return value.getFloatValue();
            else if (fieldNo == 4)
                return value.getIntValue();
            else if (fieldNo == 5)
                return value.getLongValue();
            else if (fieldNo == 6)
                return value.getBoolValue();
            //
            //  7 = null (handled above)
            //  8 = geom (not usually an attribute)
            //
            else if (fieldNo == 9)
                return Byte.valueOf((byte)value.getByteValue());
            else if (fieldNo == 10)
                return  (short) value.getShortValue();
            else if (fieldNo == 11)
                return value.getCharValue().charAt(0);
            else if (fieldNo == 12)
                return UUID.fromString(value.getUuidValue());
            else if (fieldNo == 13)
                return new java.util.Date(value.getDatetimeValue());
            else if (fieldNo == 14)
                return new java.sql.Date(value.getDateValue());
            else if (fieldNo == 15)
                return new java.sql.Time(value.getTimeValue());
            else if (fieldNo == 16) {
                com.google.protobuf.Timestamp t = value.getTimestampValue();
                long sec = t.getSeconds();
                long nano = t.getNanos();
                Instant i = Instant.ofEpochSecond(sec,nano);
                return  java.sql.Timestamp.from(i);
            }


            // 8 shouldn't happen (multi-geom)
            throw new Exception("couldn't parse oneof 'Value' field # " + fieldNo);
        }
    }
}
