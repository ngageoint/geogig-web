/* Copyright (c) 2018 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan - initial implementation
 */
package org.geogig.server.service.branch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.model.impl.RevFeatureBuilder;
import org.locationtech.geogig.plumbing.DiffFeature;
import org.locationtech.geogig.plumbing.FindTreeChild;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.plumbing.diff.FeatureDiff;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.storage.AutoCloseableIterator;
import org.locationtech.geogig.storage.BulkOpListener;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.ObjectStore;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ConflictFeatureIterator implements AutoCloseableIterator<ConflictTuple> {

    private PeekingIterator<Conflict> conflicts;

    private final ObjectStore store;

    private Iterator<ConflictTuple> nextBatch;

    private boolean closed;

    private ConflictTuple next;

    private int getAllBatchSize = 1_000;

    private final FTCache ftc;

    public ConflictFeatureIterator(Iterator<Conflict> conflicts, Context context) {
        this.conflicts = Iterators.peekingIterator(conflicts);
        this.store = context.objectDatabase();
        this.ftc = new FTCache(context);
    }

    public @Override void close() {
        closed = true;
        conflicts = null;
        nextBatch = null;
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

    public @Override ConflictTuple next() {
        if (closed) {
            throw new IllegalStateException("Iterator is closed");
        }
        final ConflictTuple curr;
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

    private @Nullable ConflictTuple computeNext() {
        if (nextBatch != null && nextBatch.hasNext()) {
            return nextBatch.next();
        }
        if (!conflicts.hasNext()) {
            return null;
        }

        final int queryBatchSize = this.getAllBatchSize;

        List<Conflict> nextEntries = Iterators.partition(this.conflicts, queryBatchSize).next();
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

    private Iterator<ConflictTuple> createBatch(List<Conflict> entries,
            Map<ObjectId, RevFeature> values) {

        return Iterators.filter(
                Iterators.transform(entries.iterator(), e -> toDiffObject(e, values)),
                Predicates.notNull());
    }

    private static final RevFeature NULL = RevFeatureBuilder.builder().build();

    private @Nullable ConflictTuple toDiffObject(Conflict e, Map<ObjectId, RevFeature> values) {
        ObjectId ancestorId = e.getAncestor();
        ObjectId theirsId = e.getTheirs();
        ObjectId oursId = e.getOurs();

        RevFeature ancestor = ancestorId.isNull() ? null : values.getOrDefault(ancestorId, NULL);
        RevFeature theirs = theirsId.isNull() ? null : values.getOrDefault(theirsId, NULL);
        RevFeature ours = oursId.isNull() ? null : values.getOrDefault(oursId, NULL);

        if (ancestor == NULL || theirs == NULL || ours == NULL) {
            return null;
        }
        List<String> conflictAttributes = getConflictAttributes(e.getPath(), ancestor, ours,
                theirs);
        return new ConflictTuple(e, ancestor, theirs, ours, conflictAttributes);
    }

    private List<String> getConflictAttributes(String path, RevFeature ancestor, RevFeature ours,
            RevFeature theirs) {
        final RevFeatureType type = ftc.getFeatureType(NodeRef.parentPath(path));

        FeatureDiff oursDiff = DiffFeature.compare(path, ancestor, ours, type, type);
        FeatureDiff theirsDiff = DiffFeature.compare(path, ancestor, theirs, type, type);

        Set<PropertyDescriptor> oursChangedProps = oursDiff.getDiffs().keySet();
        Set<PropertyDescriptor> theirsChangedProps = theirsDiff.getDiffs().keySet();
        SetView<PropertyDescriptor> bothModified = Sets.intersection(oursChangedProps,
                theirsChangedProps);

        List<String> conflictAtts = bothModified.stream().map(p -> p.getName().getLocalPart())
                .collect(Collectors.toList());

        return conflictAtts;
    }

    /**
     * {@link RevFeatureType} cache based on conflict's path.
     * <p>
     * WARN! assuming the feature types are the same at either end of the conflict! should check for
     * ORIG_HEAD and MERGE_HEAD instead!
     */
    private static @RequiredArgsConstructor class FTCache {
        private final @NonNull Context context;

        private Map<String, RevFeatureType> cache = new ConcurrentHashMap<>();

        public RevFeatureType getFeatureType(final @NonNull String treePath) {
            RevFeatureType type = cache.computeIfAbsent(treePath, this::load);
            return type;
        }

        private RevFeatureType load(String path) {
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
    }
}