package org.geogig.server.service.presentation;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.service.feature.FeatureModelDomainBridge;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.CommitDiffSummary;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.ObjectType;
import org.geogig.web.model.Person;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.RevisionObject;
import org.geogig.web.model.RevisionTag;
import org.geogig.web.model.RevisionTree;
import org.geogig.web.model.TreeBucket;
import org.geogig.web.model.TreeNode;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;
import org.geogig.web.model.Value;
import org.geogig.web.model.ValueType;
import org.geotools.util.Converters;
import org.locationtech.geogig.model.Bucket;
import org.locationtech.geogig.model.FieldType;
import org.locationtech.geogig.model.Node;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.model.RevObject.TYPE;
import org.locationtech.geogig.model.RevPerson;
import org.locationtech.geogig.model.RevTag;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.model.impl.CommitBuilder;
import org.locationtech.geogig.model.impl.RevFeatureBuilder;
import org.locationtech.geogig.model.impl.RevPersonBuilder;
import org.locationtech.geogig.model.impl.RevTagBuilder;
import org.locationtech.geogig.model.impl.RevTreeBuilder;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.jts.geom.Envelope;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Converts revision objects to and from geogig's {@link RevObject object model} to Swagger's
 *
 */
public class GeogigObjectModelBridge {

    public static String resolveCommitterName(UserInfo issuer) {
        UserInfoPrivateProfile pp = issuer.getPrivateProfile();
        String authorName = pp == null || Strings.isNullOrEmpty(pp.getFullName())
                ? issuer.getIdentity()
                : pp.getFullName();
        return authorName;
    }

    public static @Nullable String resolveCommitterEmail(UserInfo issuer) {
        UserInfoPrivateProfile pp = issuer.getPrivateProfile();
        String authorEmail = pp == null ? null : pp.getEmailAddress();
        return authorEmail;
    }

    public static RevObject map(RevisionObject obj) {
        requireNonNull(obj);

        final ObjectType objectType = obj.getObjectType();
        switch (objectType) {
        case REVISIONCOMMIT:
            return (RevCommit) toCommit((RevisionCommit) obj);
        case REVISIONTREE:
            return (RevTree) toTree((RevisionTree) obj);
        case REVISIONFEATURE:
            return (RevFeature) toFeature((RevisionFeature) obj);
        case REVISIONFEATURETYPE:
            return (RevFeatureType) toFeatureType((RevisionFeatureType) obj);
        case REVISIONTAG:
            return (RevTag) toTag((RevisionTag) obj);
        default:
            throw new AssertionError("Unknown or invalid object type: " + objectType);
        }
    }

    public static RevisionObject map(RevObject obj) {
        final TYPE type = obj.getType();
        switch (type) {
        case COMMIT:
            return (RevisionCommit) toCommit((RevCommit) obj);
        case TREE:
            return (RevisionTree) toTree((RevTree) obj);
        case FEATURE:
            return (RevisionFeature) toFeature((RevFeature) obj);
        case FEATURETYPE:
            return (RevisionFeatureType) toFeatureType((RevFeatureType) obj);
        case TAG:
            return (RevisionTag) toTag((RevTag) obj);
        default:
            throw new AssertionError("Unknown or invalid object type: " + type);
        }
    }

    public static @Nullable ObjectId toId(@Nullable String hexId) {
        return hexId == null ? null : ObjectId.valueOf(hexId);
    }

    public @Nullable static String toId(@Nullable ObjectId id) {
        return id == null ? null : id.toString();
    }

    public static List<ObjectId> toObjectIds(List<String> parentIds) {
        return Lists.transform(parentIds, s -> toId(s));
    }

    public static List<String> toStringIds(List<ObjectId> parentIds) {
        return Lists.transform(parentIds, s -> toId(s));
    }

    public static RevFeature toFeature(RevisionFeature obj) {
        RevFeatureBuilder builder = RevFeatureBuilder.builder();
        obj.getValues().forEach((v) -> builder.addValue(toObject(v)));

        ObjectId id = toId(obj.getId());
        RevFeature feature = builder.build(id);
        return feature;
    }

    public static @Nullable RevisionFeature toFeature(@Nullable RevFeature obj) {
        if (obj == null)
            return null;
        RevisionFeature f = (RevisionFeature) new RevisionFeature()//
                .objectType(ObjectType.REVISIONFEATURE)//
                .id(toId(obj.getId()));

        obj.forEach((o) -> f.addValuesItem(toValue(o)));
        return f;
    }

    public static Object toObject(Value v) {
        ValueType valueType = v.getValueType();
        FieldType fieldType = FieldType.valueOf(valueType.ordinal());
        Class<?> binding = fieldType.getBinding();

        Object object = Converters.convert(v.getValue(), binding);
        return object;
    }

    @SuppressWarnings("unchecked")
    public static Value toValue(Object o) {
        FieldType fieldType = FieldType.forValue(o);
        ValueType valueType = ValueType.fromValue(fieldType.toString());
        Object value;
        switch (valueType) {
        case NULL:
        case BOOLEAN:
        case BYTE:
        case SHORT:
        case INTEGER:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case STRING:
        case BOOLEAN_ARRAY:
        case BYTE_ARRAY:
        case SHORT_ARRAY:
        case INTEGER_ARRAY:
        case LONG_ARRAY:
        case FLOAT_ARRAY:
        case DOUBLE_ARRAY:
        case STRING_ARRAY:
        case CHAR:
        case CHAR_ARRAY:
            value = o;
            break;

        case UUID:
        case BIG_INTEGER:
        case BIG_DECIMAL:
        case POINT:
        case LINESTRING:
        case POLYGON:
        case MULTIPOINT:
        case MULTILINESTRING:
        case MULTIPOLYGON:
        case GEOMETRYCOLLECTION:
        case GEOMETRY:
            value = o.toString();
            break;
        case DATETIME:
        case DATE:
        case TIME:
        case TIMESTAMP:
            value = Converters.convert(o, String.class);
        case MAP:
            value = toAPIMap((Map<String, Object>) o);
            break;
        case ENVELOPE_2D:
            value = toBounds((Envelope) o);
            break;
        default:
            throw new IllegalArgumentException(
                    "Uknown ValueType '" + valueType + "' for FieldType '" + fieldType + "'");
        }

        Value apiValue = new Value().valueType(valueType).value(value);
        return apiValue;
    }

    public static RevTree toTree(RevisionTree o) {
        ObjectId id = toId(o.getId());
        long size = o.getSize().longValue();
        int childTreeCount = o.getNumTrees().intValue();
        ImmutableList<Node> trees = toNodes(o.getTrees());
        ImmutableList<Node> features = toNodes(o.getFeatures());
        SortedMap<Integer, Bucket> buckets = toBuckets(o.getBuckets());

        RevTree tree = RevTreeBuilder.create(id, size, childTreeCount, trees, features, buckets);

        return tree;
    }

    public static RevisionTree toTree(RevTree o) {
        RevisionTree tree = new RevisionTree()//
                .size(Long.valueOf(o.size()))//
                .numTrees(Integer.valueOf(o.numTrees()))//
                .features(toTreeNodes(o.features()))//
                .trees(toTreeNodes(o.trees()))//
                .buckets(toTreeBuckets(o.buckets()));

        tree.id(toId(o.getId())).objectType(ObjectType.REVISIONTREE);
        return tree;
    }

    public static ImmutableList<Node> toNodes(List<TreeNode> treeNodes) {
        ImmutableList<Node> nodes;
        nodes = ImmutableList.copyOf(Lists.transform(treeNodes, n -> toNode(n)));
        return nodes;
    }

    public static List<TreeNode> toTreeNodes(ImmutableList<Node> nodes) {
        List<TreeNode> treeNodes = new ArrayList<>(Lists.transform(nodes, n -> toNode(n)));
        return treeNodes;
    }

    public static Node toNode(TreeNode tn) {
        Preconditions.checkArgument(tn.getType() == ObjectType.REVISIONFEATURE
                || tn.getType() == ObjectType.REVISIONTREE);

        String name = tn.getName();
        ObjectId oid = toId(tn.getObjectId());
        ObjectId metadataId = Optional.fromNullable(toId(tn.getMetadataId())).or(ObjectId.NULL);
        TYPE type = tn.getType() == ObjectType.REVISIONFEATURE ? TYPE.FEATURE : TYPE.TREE;
        Envelope bounds = toBounds(tn.getBounds());
        Map<String, Object> extraData = toExtraData(tn.getExtraData());
        Node node = Node.create(name, oid, metadataId, type, bounds, extraData);
        return node;
    }

    public static TreeNode toNode(Node n) {
        Preconditions.checkArgument(n.getType() == TYPE.FEATURE || n.getType() == TYPE.TREE);
        TreeNode tn = new TreeNode()//
                .name(n.getName())//
                .objectId(toId(n.getObjectId()))//
                .metadataId(toId(n.getMetadataId().orNull()))//
                .type(n.getType() == TYPE.FEATURE ? ObjectType.REVISIONFEATURE
                        : ObjectType.REVISIONTREE)//
                .bounds(toBounds(n.bounds().orNull()))//
                .extraData(toAPIMap(n.getExtraData()));
        return tn;
    }

    public static @Nullable Map<String, Object> toExtraData(
            @Nullable Map<String, Object> extraData) {
        // TODO Auto-generated method stub
        return null;
    }

    public static @Nullable Map<String, Object> toAPIMap(@Nullable Map<String, Object> extraData) {
        return null;
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

    public static @Nullable SortedMap<Integer, Bucket> toBuckets(
            @Nullable List<TreeBucket> buckets) {
        if (null == buckets || buckets.isEmpty()) {
            return null;
        }
        ImmutableSortedMap.Builder<Integer, Bucket> builder = ImmutableSortedMap.naturalOrder();
        for (TreeBucket tb : buckets) {
            Integer index = tb.getIndex();
            Bucket b = Bucket.create(toId(tb.getObjectId()), toBounds(tb.getBounds()));
            builder.put(index, b);
        }
        return builder.build();
    }

    public static @Nullable List<TreeBucket> toTreeBuckets(
            @Nullable SortedMap<Integer, Bucket> buckets) {

        List<TreeBucket> tbs = new ArrayList<>(buckets.size());
        buckets.forEach((index, bucket) -> {
            TreeBucket tb = new TreeBucket().index(index);
            tb.objectId(toId(bucket.getObjectId()));
            tb.bounds(toBounds(bucket.bounds().orNull()));
            tbs.add(tb);
        });
        return tbs;
    }

    public static @Nullable CommitDiffSummary toCommitDiffSummary(Map<String,List<RevCommit>> summary) {
        if (summary == null) {
            return null;
        }
        CommitDiffSummary result= new CommitDiffSummary();
        result.setCommitsAhead(
                    Lists.newArrayList(
                            Iterators.transform(summary.get("ahead").iterator(),GeogigObjectModelBridge::toCommit)));
        result.setCommitsBehind(
                    Lists.newArrayList(
                            Iterators.transform(summary.get("behind").iterator(),GeogigObjectModelBridge::toCommit)));
        return result;
    }

    public static @Nullable RevisionCommit toCommit(@Nullable RevCommit obj) {
        if (obj == null) {
            return null;
        }
        RevisionCommit c = (RevisionCommit) new RevisionCommit()//
                .treeId(toId(obj.getTreeId()))//
                .parentIds(toStringIds(obj.getParentIds()))//
                .author(toPerson(obj.getAuthor()))//
                .committer(toPerson(obj.getCommitter()))//
                .message(obj.getMessage())//
                .id(toId(obj.getId()))//
                .objectType(ObjectType.REVISIONCOMMIT);
        return c;
    }

    public static Person toPerson(RevPerson p) {
        Person person = new Person()//
                .name(p.getName().orNull())//
                .email(p.getEmail().orNull())//
                .timestamp(p.getTimestamp())//
                .timezoneOffset(p.getTimeZoneOffset());
        return person;
    }

    public static RevPerson toPerson(Person p) {
        String name = p.getName();
        String email = p.getEmail();
        long timeStamp = p.getTimestamp().longValue();
        int timeZoneOffset = p.getTimezoneOffset().intValue();
        RevPerson person = RevPersonBuilder.build(name, email, timeStamp, timeZoneOffset);
        return person;
    }

    public static RevCommit toCommit(RevisionCommit o) {
        RevCommit commit = new CommitBuilder()//
                .setAuthor(o.getAuthor().getName())//
                .setAuthorEmail(o.getAuthor().getEmail())//
                .setAuthorTimestamp(o.getAuthor().getTimestamp().longValue())//
                .setAuthorTimeZoneOffset(o.getAuthor().getTimezoneOffset().intValue())//
                .setCommitter(o.getCommitter().getName())//
                .setCommitterEmail(o.getCommitter().getEmail())//
                .setCommitterTimestamp(o.getCommitter().getTimestamp().longValue())//
                .setCommitterTimeZoneOffset(o.getCommitter().getTimezoneOffset().intValue())//
                .setMessage(o.getMessage())//
                .setTreeId(toId(o.getTreeId()))//
                .setParentIds(toObjectIds(o.getParentIds()))//
                .build();

        return commit;
    }

    private @Nullable static RevTag toTag(@Nullable RevisionTag obj) {
        if (obj == null) {
            return null;
        }
        String name = obj.getName();
        ObjectId commitId = toId(obj.getCommitId());
        String message = obj.getMessage();
        RevPerson tagger = toPerson(obj.getTagger());
        RevTag tag = RevTagBuilder.build(name, commitId, message, tagger);
        return tag;
    }

    public static RevisionTag toTag(RevTag obj) {
        RevisionTag revisionTag = new RevisionTag();
        revisionTag.name(obj.getName());
        revisionTag.message(obj.getMessage());
        revisionTag.commitId(toId(obj.getCommitId()));
        revisionTag.tagger(toPerson(obj.getTagger()));
        return revisionTag;
    }

    public static RevFeatureType toFeatureType(RevisionFeatureType obj) {
        return FeatureModelDomainBridge.toFeatureType(obj);
    }

    public static RevisionFeatureType toFeatureType(RevFeatureType t) {
        return FeatureModelDomainBridge.toFeatureType(t);
    }

    public static ConflictInfo toConflict(Conflict c) {
        ConflictInfo ci = new ConflictInfo();
        ci.setPath(c.getPath());
        ci.setAncestor(toId(c.getAncestor()));
        ci.setOurs(toId(c.getOurs()));
        ci.setTheirs(toId(c.getTheirs()));
        return ci;
    }

}
