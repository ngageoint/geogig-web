package org.geogig.server.service.feature;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geogig.server.service.transaction.TransactionService;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.FeatureQuery.ResultTypeEnum;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.UpdateFeaturesRequest;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geogig.web.model.geotools.GeogigFeatureReaderGeotoolsAdapter;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.Hints;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.ScreenMap;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.geogig.data.FindFeatureTypeTrees;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStore.ChangeType;
import org.locationtech.geogig.geotools.data.GeogigDiffFeatureSource;
import org.locationtech.geogig.geotools.data.GeogigFeatureSource;
import org.locationtech.geogig.geotools.data.GeogigFeatureStore;
import org.locationtech.geogig.geotools.data.reader.WalkInfo;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.plumbing.RevParse;
import org.locationtech.geogig.porcelain.BranchResolveOp;
import org.locationtech.geogig.porcelain.index.CreateQuadTree;
import org.locationtech.geogig.porcelain.index.Index;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.WorkingTree;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.locationtech.geogig.repository.impl.SpatialOps;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("FeatureService")
public class FeatureService {

    private @Autowired TransactionService transactions;

    /**
     * @param user
     * @param repo
     * @param txId
     * @param schema
     * @throws IllegalArgumentException
     * @throws NoSuchElementException
     * @return
     * @throws RepositoryConnectionException
     */
    public LayerInfo create(//@formatter:off
            @NonNull String user, 
            @NonNull String repo,
            @NonNull UUID txId,
            @NonNull RevisionFeatureType schema)
            throws NoSuchElementException, RepositoryConnectionException {//@formatter:on

        checkArgument(schema.getName() != null, "Layer name not provided");

        final GeogigTransaction tx = transactions.resolve(user, repo, txId);

        final Ref currentBranch = getCurrentBranch(tx);

        final String branch = currentBranch.localName();
        final String layerName = schema.getName();

        RevFeatureType revType = FeatureModelDomainBridge.toFeatureType(schema);
        FeatureType featureType = revType.type();

        // do not call DataStore.createSchema() since it automatically makes a commit (as it can't
        // be called inside a transcation). Instead, create the layer in the WORK_HEAD directly
        // inside the transaction
        log.debug("Creating layer {}/{}/{}/{}, tx: {}", user, repo, branch, layerName, txId);
        NodeRef treeRef = tx.workingTree().createTypeTree(layerName, featureType);
        RevFeatureType savedRevType = tx.objectDatabase().getFeatureType(treeRef.getMetadataId());

        log.debug("Created empty layer {}/{}/{}/{}, initializing spatial index, tx: {}", user, repo,
                branch, layerName, txId);
        try {
            Index index = tx.command(CreateQuadTree.class).setTypeTreeRef(treeRef).call();
            log.debug("Layer created {}/{}/{}/{}, tx: {}, index: {}", user, repo, branch, layerName,
                    txId, index.indexTreeId());
        } catch (RuntimeException e) {
            log.warn(
                    "Error creating index for {}/{}/{}/{}, tx: {}. "
                            + "Returning layer nontheless.",
                    user, repo, branch, layerName, txId, e);
        }
        LayerInfo newLayer = toLayerInfo(savedRevType);
        return newLayer;
    }

    private Ref getCurrentBranch(final Context context) {
        final Ref currentBranch = context.command(BranchResolveOp.class).call()
                .orElseThrow(() -> new IllegalStateException(
                        "The repository is in a dettached state, no branch is currently checked out"));
        return currentBranch;
    }

    public LayerInfo getLayer(//@formatter:off
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer,
            @Nullable String head, 
            @Nullable UUID txId) {
        //@formatter:on
        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
        } catch (NoSuchElementException | RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        head = resolveHead(context, head);

        List<NodeRef> featureTypeNodes = findLayerRefNodes(context, head);

        NodeRef node = featureTypeNodes.stream().filter((n) -> layer.equals(n.name())).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Layer " + layer + " not found"));

        LayerInfo layerInfo = toLayer(context, node);

        return layerInfo;
    }

    private LayerInfo toLayer(Context context, NodeRef node) {
        RevFeatureType revType = context.objectDatabase().getFeatureType(node.getMetadataId());
        LayerInfo layerInfo = toLayerInfo(revType);

        if (!RevTree.EMPTY_TREE_ID.equals(node.getObjectId())) {
            RevTree tree = context.objectDatabase().getTree(node.getObjectId());
            layerInfo.setSize(tree.size());
            BoundingBox bounds = FeatureModelDomainBridge.toBounds(SpatialOps.boundsOf(tree));
            layerInfo.setBounds(bounds);
        }
        return layerInfo;
    }

    public List<LayerInfo> getLayers(//@formatter:off
            @NonNull String user, 
            @NonNull String repo,
            @Nullable UUID txId, 
            @Nullable String head) {
        //@formatter:on
        try {
            Context context = transactions.resolveContext(user, repo, txId);
            head = resolveHead(context, head);
            List<NodeRef> featureTypeNodes = findLayerRefNodes(context, head);
            return Lists.transform(featureTypeNodes, (node) -> toLayer(context, node));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> splitQualifiedHead(final String qualifiedHead) {
        List<String> parts = Splitter.on(':').omitEmptyStrings().splitToList(qualifiedHead);
        checkArgument(parts.size() == 1 || parts.size() == 3,
                "Invalid qualified head format, expected '[user:repo:]tree-ish', got '%s'",
                qualifiedHead);
        return parts;
    }

    /**
     * @param context
     * @param qualifiedHead {@code [user:repo:]tree-ish}
     * @return {@code unqualifiedContext} if no {@code user:repo} is provided, or the resolved
     *         repository context if it was
     */
    private Context resolveContext(//
            @NonNull Context unqualifiedContext, //
            @NonNull String qualifiedHead) {

        List<String> parts = splitQualifiedHead(qualifiedHead);
        if (1 == parts.size()) {
            return unqualifiedContext;
        }
        String user = parts.get(0);
        String repo = parts.get(1);
        UUID txId = null;
        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        return context;
    }

    private String resolveHead(Context context, @Nullable String head) {
        if (Strings.isNullOrEmpty(head)) {
            return Ref.WORK_HEAD;
        }
        final List<String> parts = splitQualifiedHead(head);
        head = parts.get(parts.size() - 1);
        ObjectId headId = context.command(RevParse.class).setRefSpec(head).call().orNull();
        if (headId == null) {
            log.debug("head argument '{}' does not resolve to any repository object", head);
            throw new NoSuchElementException(head + " not found");
        }
        head = headId.toString();

        log.trace("head argument '{}' resolved to '{}'", head, head);
        return head;
    }

    private List<NodeRef> findLayerRefNodes(@NonNull Context context, @NonNull String head) {
        List<NodeRef> nodes = context.command(FindFeatureTypeTrees.class).setRootTreeRef(head)
                .call();
        return nodes;
    }

    private LayerInfo toLayerInfo(RevFeatureType savedRevType) {
        RevisionFeatureType savedTypeInfo = FeatureModelDomainBridge.toFeatureType(savedRevType);
        FeatureType schema = savedRevType.type();
        LayerInfo newLayer = new LayerInfo();
        String head = null;// tx.command(commandClass);
        BoundingBox bounds = new BoundingBox();
        newLayer.name(schema.getName().getLocalPart()).type(savedTypeInfo).bounds(bounds).size(0L)
                .head(head);
        return newLayer;
    }

    public GeogigFeatureCollection getFeatures(//@formatter:off
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer,
            @NonNull FeatureQuery query,
            @Nullable UUID txId) {

        final Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        return getFeatures(context, layer, query);
    }
    
    public GeogigFeatureCollection getFeatures(//@formatter:off
            @NonNull UUID repo, 
            @Nullable UUID txId, 
            @NonNull String layer,
            @NonNull FeatureQuery query) {
        //@formatter:on

        final Context context;
        try {
            context = transactions.resolveContext(repo, txId);
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }
        return getFeatures(context, layer, query);
    }

    public GeogigFeatureCollection getConflictingFeatures(@NonNull Context context,
            @NonNull String layer, @NonNull FeatureQuery query) {
        try {
            Iterator<Conflict> conflicts = context.conflictsDatabase().getByPrefix(null, layer);
            ConflictFeatureIterator conflictsIterator = new ConflictFeatureIterator(conflicts,
                    context, layer);
            return GeogigFeatureCollection.of(conflictsIterator.getGeogigFeatureType(),
                    conflictsIterator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GeogigFeatureCollection getFeatures(//@formatter:off
            @NonNull Context context, 
            @NonNull String layer,
            @NonNull FeatureQuery query) {
        if ( (query.isConflicts() != null) && (query.isConflicts().booleanValue()) )
            return getConflictingFeatures(context,layer,query);
        //@formatter:on
        final GeoGigDataStore store = new GeoGigDataStore(context);
        try {
            store.setCloseOnDispose(false);
            final String head = resolveHead(context, query.getHead());
            store.setHead(head);

            SimpleFeatureSource source;
            try {
                if (Strings.isNullOrEmpty(query.getOldHead())) {
                    source = store.getFeatureSource(layer);
                } else {
                    String fullOldHead = query.getOldHead();
                    if ((query.getOldHeadRepo() != null) && (query.getOldHeadUser() != null)) {
                        fullOldHead = query.getOldHeadUser() + ":" + query.getOldHeadRepo() + ":"
                                + fullOldHead;
                    }
                    final Context oldObjectsContext = resolveContext(context, fullOldHead);
                    final @Nullable String oldHead = resolveHead(oldObjectsContext,
                            query.getOldHead());

                    GeogigDiffFeatureSource diffSource = store.getDiffFeatureSource(layer, oldHead,
                            oldObjectsContext, ChangeType.ALL);
                    Boolean flattenDiffSchema = query.isFlattenDiffSchema();
                    if (flattenDiffSchema != null && flattenDiffSchema.booleanValue()) {
                        diffSource.setFlattenSchema(true);
                    }
                    source = diffSource;
                }
            } catch (IOException e) {
                throw new NoSuchElementException("Layer " + layer + " not found");
            }
            SimpleFeatureType schema = source.getSchema();

            final Query gtQuery;
            if (ResultTypeEnum.FIDS == query.getResultType()) {
                gtQuery = new Query(Query.FIDS);
            } else {
                gtQuery = GeoToolsDomainBridge.toQuery(schema, query);
            }
            gtQuery.getHints().put(GeogigFeatureSource.WALK_INFO_KEY, Boolean.TRUE);

            final GeogigFeatureCollection collection;

            final ResultTypeEnum resultType = Optional.ofNullable(query.getResultType())
                    .orElse(ResultTypeEnum.FEATURES);
            try {
                switch (resultType) {
                case BOUNDS:
                    ReferencedEnvelope bounds = source.getBounds(gtQuery);
                    collection = GeogigFeatureCollection
                            .boundsOnly(GeoToolsDomainBridge.toBounds(bounds));
                    break;
                case COUNT:
                    int count = source.getCount(gtQuery);
                    collection = GeogigFeatureCollection.sizeOnly(count);
                    break;
                case FIDS:
                case FEATURES:
                    configureScreenMap(query, gtQuery, schema);
                    SimpleFeatureCollection features = source.getFeatures(gtQuery);
                    GeogigFeatureReaderGeotoolsAdapter featureReader;
                    featureReader = new GeogigFeatureReaderGeotoolsAdapter(features);
                    RevisionFeatureType type = featureReader.getType();
                    collection = GeogigFeatureCollection.streamingCollection(type, featureReader);
                    break;
                default:
                    throw new IllegalArgumentException();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final @Nullable WalkInfo walkInfo = GeogigFeatureSource.WALK_INFO.get();
            if (null != walkInfo) {
                Map<String, String> extraData = collection.getExtraData();
                setGetFeaturesExtraData(walkInfo, extraData);
            }
            return collection;
        } finally {
            GeogigFeatureSource.WALK_INFO.remove();
            store.dispose();
        }
    }

    private void setGetFeaturesExtraData(final WalkInfo walkInfo, Map<String, String> extraData) {
        String leftIndexId;
        String rightIndexId;
        leftIndexId = walkInfo.leftIndex.map(i -> i.indexTreeId()).map(id -> id.toString())
                .orElse(null);
        rightIndexId = walkInfo.rightIndex.map(i -> i.indexTreeId()).map(id -> id.toString())
                .orElse(null);

        extraData.put("using-index", String.valueOf(walkInfo.diffUsesIndex));
        extraData.put("left-index-sha", leftIndexId);
        extraData.put("right-index-sha", rightIndexId);
        extraData.put("left-tree-sha", walkInfo.leftTree.toString());
        extraData.put("right-tree-sha", walkInfo.rightTree.toString());
        String leftRef = walkInfo.leftRef
                .map(ref -> String.format("%s:%s", ref.getName(), ref.getObjectId())).orElse(null);
        String rightRef = walkInfo.rightRef
                .map(ref -> String.format("%s:%s", ref.getName(), ref.getObjectId())).orElse(null);

        extraData.put("left-ref", leftRef);
        extraData.put("right-ref", rightRef);
    }

    private void configureScreenMap(FeatureQuery query, final Query gtQuery,
            SimpleFeatureType schema) {

        ScreenMap screenMap = (ScreenMap) gtQuery.getHints().get(Hints.SCREENMAP);
        final Integer w = query.getScreenWidth();
        final Integer h = query.getScreenHeight();
        if (screenMap != null && w != null && h != null && w > 0 && h > 0) {
            SRS outputCrs = query.getOutputCrs();
            CoordinateReferenceSystem targetCrs = outputCrs == null
                    ? schema.getCoordinateReferenceSystem()
                    : GeoToolsDomainBridge.toCrs(outputCrs);
            ReferencedEnvelope queryBounds;
            {// ExtractBoundsFilterVisitor returns Envelope instead of ReferencedEnvelope when
             // visiting an OR filter, work around it
                Envelope ee = (Envelope) gtQuery.getFilter()
                        .accept(ExtractBoundsFilterVisitor.BOUNDS_VISITOR, targetCrs);
                if (ee instanceof ReferencedEnvelope) {
                    queryBounds = (ReferencedEnvelope) ee;
                } else {
                    queryBounds = new ReferencedEnvelope(targetCrs);
                    queryBounds.expandToInclude(ee);
                }
            }
            if (queryBounds != null && !queryBounds.isNull()
                    && !Double.isInfinite(queryBounds.getMinX())) {

                Envelope bbox = queryBounds;
                {// REVISIT: this should be part of GeoGigFeatureSource?
                    CoordinateReferenceSystem sourceCrs = schema.getCoordinateReferenceSystem();
                    if (!CRS.equalsIgnoreMetadata(sourceCrs, queryBounds)) {
                        try {
                            GeneralEnvelope sourceCrsBounds = CRS.transform(queryBounds, sourceCrs);
                            bbox = new ReferencedEnvelope(sourceCrsBounds);
                        } catch (TransformException e) {
                            e.printStackTrace();
                        }
                    }
                }

                final Rectangle screenSize = new Rectangle(w, h);
                final double scaleDenominator = calculateScale(bbox, screenSize, schema);
                MathTransform mt = null;
                double[] spans = null;
                if (scaleDenominator > 50_000) {
                    try {
                        final double generalizationDistance = 0.5;
                        AffineTransform worldToScreen;
                        worldToScreen = worldToScreenTransform(bbox, screenSize);
                        mt = ProjectiveTransform.create(worldToScreen);
                        spans = Decimator.computeGeneralizationDistances(mt.inverse(), screenSize,
                                generalizationDistance);
                    } catch (TransformException e) {
                        log.info("Error computing map transform for screenmap", e);
                    }
                }
                if (mt != null && spans != null) {
                    screenMap.setTransform(mt);
                    screenMap.setSpans(spans[0], spans[1]);
                } else {
                    gtQuery.getHints().remove(Hints.SCREENMAP);
                }
            }
        }
    }

    private double calculateScale(Envelope bbox, Rectangle screenSize, SimpleFeatureType schema) {
        ReferencedEnvelope bounds = null;
        if (bbox instanceof ReferencedEnvelope
                && null != ((ReferencedEnvelope) bbox).getCoordinateReferenceSystem()) {
            bounds = (ReferencedEnvelope) bbox;
        } else {
            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            bounds = new ReferencedEnvelope(crs);
            bounds.init(bbox);
        }
        double scale;
        try {
            scale = RendererUtilities.calculateScale(bounds, screenSize.width, screenSize.height,
                    null);
        } catch (TransformException | FactoryException e) {
            log.info("Error computing scale denominator", e);
            scale = -1d;
        }

        return scale;
    }

    static AffineTransform worldToScreenTransform(Envelope mapExtent, Rectangle paintArea) {
        double scaleX = paintArea.getWidth() / mapExtent.getWidth();
        double scaleY = paintArea.getHeight() / mapExtent.getHeight();

        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + paintArea.getHeight();

        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);
        AffineTransform originTranslation = AffineTransform.getTranslateInstance(paintArea.x,
                paintArea.y);
        originTranslation.concatenate(at);

        return originTranslation != null ? originTranslation : at;
    }

    //@formatter:off
    public void insert(
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer,
            @NonNull GeogigFeatureCollection collection, 
            @NonNull UUID txId) {
        //@formatter:on

        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
            final Ref currentBranch = getCurrentBranch(context);
            final String branch = currentBranch.localName();
            GeoGigDataStore store = new GeoGigDataStore(context);
            try {
                store.setCloseOnDispose(false);
                store.setHead(Ref.WORK_HEAD);

                SimpleFeatureCollection source;
                GeogigFeatureStore target;

                target = store.getFeatureStore(layer);
                target.setReturnFidsOnInsert(false);

                SimpleFeatureType schema = target.getSchema();
                source = GeoToolsDomainBridge.toFeatureCollection(collection, schema);

                log.info("Inserting features to layer {}/{}/{}/{}", user, repo, branch, layer);
                Stopwatch sw = Stopwatch.createStarted();
                target.addFeatures(source);
                log.info("Inserted features to {}/{}/{}/{}, {}", user, repo, branch, layer,
                        sw.stop());
            } finally {
                store.dispose();
            }
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    //@formatter:off
    public void update(
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer,
            @NonNull UUID txId,
            @NonNull UpdateFeaturesRequest query) {
        //@formatter:on

        GeogigFeature prototype = (GeogigFeature) query.getPrototype();
        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
            GeoGigDataStore store = new GeoGigDataStore(context);
            try {
                store.setCloseOnDispose(false);
                store.setHead(Ref.WORK_HEAD);
                SimpleFeatureStore source = (SimpleFeatureStore) store.getFeatureSource(layer);
                SimpleFeatureType schema = source.getSchema();
                Filter filter = GeoToolsDomainBridge.toFilter(schema, query.getFilter());

                String[] names;
                Object[] attributeValues;
                {
                    List<String> n = new ArrayList<>();
                    List<Object> v = new ArrayList<>();
                    prototype.forEach((attname, attvalue) -> {
                        if (GeogigFeature.UNKNOWN_DEFAULT_GEOMETRY_NAME_PLACEHOLDER
                                .equals(attname)) {
                            attname = schema.getGeometryDescriptor().getLocalName();
                        }
                        n.add(attname);
                        v.add(attvalue);
                    });
                    names = n.toArray(new String[n.size()]);
                    attributeValues = v.toArray(new Object[n.size()]);
                }
                source.modifyFeatures(names, attributeValues, filter);
            } finally {
                store.dispose();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //@formatter:off
    public void delete(
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer, 
            @NonNull UUID txId, 
            @NonNull FeatureFilter filter) {
        //@formatter:on
        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
            GeoGigDataStore store = new GeoGigDataStore(context);
            try {
                store.setCloseOnDispose(false);
                store.setHead(Ref.WORK_HEAD);
                SimpleFeatureStore source = (SimpleFeatureStore) store.getFeatureSource(layer);
                Filter gtFilter = GeoToolsDomainBridge.toFilter(source.getSchema(), filter);
                source.removeFeatures(gtFilter);
            } finally {
                store.dispose();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteLayer(//@formatter:off
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull String layer, 
            @NonNull UUID txId) {//@formatter:on

        Context context;
        try {
            context = transactions.resolveContext(user, repo, txId);
            // REVISIT: removeSchema not yet supported by GeogigDataStore, do it manually

            WorkingTree workingTree = context.workingTree();
            ObjectId currentWorkHead = workingTree.getTree().getId();
            ObjectId newWorkHead = workingTree.delete(layer);
            if (currentWorkHead.equals(newWorkHead)) {
                throw new NoSuchElementException(String.format("Layer not found: %s", layer));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
