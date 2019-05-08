/* Copyright (c) 2018 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * David Blasby - initial implementation
 */
package org.geogig.server.service.feature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.geogig.data.FeatureBuilder;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.model.impl.RevFeatureBuilder;
import org.locationtech.geogig.plumbing.FindTreeChild;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.storage.AutoCloseableIterator;
import org.locationtech.geogig.storage.BulkOpListener;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.ObjectStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureTypeFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import lombok.NonNull;

public class ConflictFeatureIterator implements AutoCloseableIterator<GeogigFeature> {

        private PeekingIterator<Conflict> conflicts;

        private final ObjectStore store;

        private Iterator<GeogigFeature> nextBatch;

        private boolean closed;

        private GeogigFeature next;

        private int getAllBatchSize = 1_000;

        private FeatureBuilder featureBuilder; // converts the DB feature (RevFeature) to a SimpleFeature

        private RevFeatureType revFeatureType; // for the simpliest underlying feature in the DB (components - "theirs", "ours", "ancestor" - are of this type)

        private SimpleFeatureType conflictFeatureType;  // simple feature type for the conflict (has 3 attributes (each being a Feature) )

        private SimpleFeatureBuilder conflictFeatureTypeBuilder; // builder to make conflictFeatureType (SimpleFeatureType)

        private RevisionFeatureType conflictFeatureTypeGeogig; // WebApi model of the final FT (has 3 attributes (each being a Feature) )

        private String layername;

        public ConflictFeatureIterator(Iterator<Conflict> conflicts, Context context,
                String layername) {
                this.layername = layername;

                this.conflicts = Iterators.peekingIterator(conflicts);
                this.store = context.objectDatabase();
                this.revFeatureType = loadFT(context, layername);
                FeatureBuilder featureBuilder = new FeatureBuilder(revFeatureType);
                this.featureBuilder = featureBuilder;
                this.conflictFeatureType = buildConflicFeatureType(layername,
                        (SimpleFeatureType) revFeatureType.type());
                this.conflictFeatureTypeBuilder = new SimpleFeatureBuilder(
                        this.conflictFeatureType);
                this.conflictFeatureTypeGeogig = GeoToolsDomainBridge
                        .toFeatureType(this.conflictFeatureType);
        }

        public @Override void close() {
                closed = true;
                conflicts = null;
                nextBatch = null;
        }

        // gets a FT from the DB
        private static RevFeatureType loadFT(@NonNull Context context, String path) {
                ObjectId rootId = context.command(ResolveTreeish.class).setTreeish(Ref.HEAD).call()
                        .get();

                ObjectDatabase store = context.objectDatabase();
                RevTree root = store.getTree(rootId);
                NodeRef treeRef = context.command(FindTreeChild.class).setChildPath(path)
                        .setParent(root).call().orNull();
                Preconditions.checkNotNull(treeRef);
                ObjectId mdid = treeRef.getMetadataId();
                return store.getFeatureType(mdid);
        }

        //sets up the basic SimpleFeatureType representing the conflict
        // has 3 attributes - ancestor, ours, theirs (all Features)
        public static SimpleFeatureType buildConflicFeatureType(String typeName,
                SimpleFeatureType nativeFeatureType) {

                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                FeatureTypeFactory typeFactory = builder.getFeatureTypeFactory();

                AttributeDescriptor ancestorValDescriptor = typeFactory
                        .createAttributeDescriptor(nativeFeatureType, new NameImpl("ancestor"), 1,
                                1, true, null);
                AttributeDescriptor oursValDescriptor = typeFactory
                        .createAttributeDescriptor(nativeFeatureType, new NameImpl("ours"), 1, 1,
                                true, null);
                AttributeDescriptor theirsValDescriptor = typeFactory
                        .createAttributeDescriptor(nativeFeatureType, new NameImpl("theirs"), 1, 1,
                                true, null);

                builder.add(ancestorValDescriptor);
                builder.add(oursValDescriptor);
                builder.add(theirsValDescriptor);
                builder.setName(typeName);
                return builder.buildFeatureType();
        }

        public @Override boolean hasNext() {
                if (closed) {
                        throw new IllegalStateException("Iterator was closed already");
                }
                if (next == null) {
                        next = computeNext();
                }
                return next != null;
        }

        public @Override GeogigFeature next() {
                if (closed) {
                        throw new IllegalStateException("Iterator is closed");
                }
                final GeogigFeature curr;
                if (next == null) {
                        curr = computeNext();
                } else {
                        curr = next;
                        next = null;
                }
                if (curr == null) {
                        throw new NoSuchElementException();
                }
                return curr;
        }

        //batch getting conflicts (and the RevFeature for the 3 features involved)
        private @Nullable GeogigFeature computeNext() {
                if (nextBatch != null && nextBatch.hasNext()) {
                        return nextBatch.next();
                }
                if (!conflicts.hasNext()) {
                        return null;
                }

                final int queryBatchSize = this.getAllBatchSize;

                List<Conflict> nextEntries = Iterators.partition(this.conflicts, queryBatchSize)
                        .next();
                Set<ObjectId> entriesIds = new HashSet<>();
                nextEntries.forEach((e) -> {
                        ObjectId ancestor = e.getAncestor();
                        ObjectId ours = e.getOurs();
                        ObjectId theirs = e.getTheirs();
                        //@formatter:off
            if (!ancestor.isNull()) {entriesIds.add(ancestor);}
            if (!ours.isNull()) {entriesIds.add(ours);}
            if (!theirs.isNull()) {entriesIds.add(theirs);}
            //@formatter:on
                });

                Iterator<RevFeature> objects;
                objects = store.getAll(entriesIds, BulkOpListener.NOOP_LISTENER, RevFeature.class);

                Map<ObjectId, RevFeature> objectsById = new HashMap<>();
                objects.forEachRemaining((o) -> objectsById.putIfAbsent(o.getId(), o));
                nextBatch = createBatch(nextEntries, objectsById);
                return computeNext();
        }

        private Iterator<GeogigFeature> createBatch(List<Conflict> entries,
                Map<ObjectId, RevFeature> values) {

                return Iterators.filter(Iterators
                                .transform(entries.iterator(), e -> toConflictObject(e, values)),
                        Predicates.notNull());
        }

        // takes the 3 revFeatures, converts them to SimpleFeatures, then
        // creates the more complex SimpleFeature (3 columns - ancestor,ours,theres), then
        // creates the equivelent GeogigFeature for that SimpleFeature.
        private @Nullable GeogigFeature toConflictObject(Conflict e,
                Map<ObjectId, RevFeature> values) {
                ObjectId ancestorId = e.getAncestor();
                ObjectId theirsId = e.getTheirs();
                ObjectId oursId = e.getOurs();

                RevFeature ancestor = ancestorId.isNull() ?
                        null :
                        values.getOrDefault(ancestorId, NULL);
                RevFeature theirs = theirsId.isNull() ? null : values.getOrDefault(theirsId, NULL);
                RevFeature ours = oursId.isNull() ? null : values.getOrDefault(oursId, NULL);

                String id = getIdFromPath(e.getPath());
                SimpleFeature ancestor_f = ancestor == null ?
                        null :
                        (SimpleFeature) featureBuilder.build(id, ancestor);
                SimpleFeature theirs_f =
                        theirs == null ? null : (SimpleFeature) featureBuilder.build(id, theirs);
                SimpleFeature ours_f =
                        ours == null ? null : (SimpleFeature) featureBuilder.build(id, ours);

                conflictFeatureTypeBuilder.add(ancestor_f);
                conflictFeatureTypeBuilder.add(theirs_f);
                conflictFeatureTypeBuilder.add(ours_f);

                SimpleFeature feature = conflictFeatureTypeBuilder.buildFeature(id);
                GeogigFeature result = GeoToolsDomainBridge
                        .toFeature(conflictFeatureTypeGeogig, feature, null);

                return result;
        }

        String getIdFromPath(String path) {
                return path.substring(layername.length() + 1);
        }

        private static final RevFeature NULL = RevFeatureBuilder.builder().build();

        public RevisionFeatureType getGeogigFeatureType() {
                return this.conflictFeatureTypeGeogig;
        }

}