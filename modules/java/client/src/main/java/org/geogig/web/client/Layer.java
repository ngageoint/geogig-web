package org.geogig.web.client;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.UpdateFeaturesRequest;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.NonNull;

public class Layer {

    private final Branch branch;

    private LayerInfo layerInfo;

    Layer(@NonNull Branch branch, @NonNull LayerInfo info) {
        this.branch = branch;
        this.layerInfo = info;
    }

    public String getName() {
        return getInfo().getName();
    }

    public LayerInfo getInfo() {
        return layerInfo;
    }

    public Branch getBranch() {
        return branch;
    }

    public Repo getRepo() {
        return getBranch().getRepo();
    }

    public String getHead() {
        return getInfo().getHead();
    }

    public BoundingBox getBounds() {
        BoundingBox bounds = getInfo().getBounds();
        return bounds;
    }

    public BoundingBox getBounds(@NonNull FeatureQuery apiQuery) {
        return branch.featureService().getBounds(getName(), apiQuery);
    }

    public long getSize() {
        return getInfo().getSize().longValue();
    }

    public long getSize(FeatureQuery apiQuery) {
        return branch.featureService().getSize(getName(), apiQuery);
    }

    public RevisionFeatureType getType() {
        return getInfo().getType();
    }

    public GeogigFeatureCollection getFeatures() {
        return getFeatures(new FeatureQuery());
    }

    public GeogigFeatureCollection getFeatures(@NonNull FeatureQuery apiQuery) {
        if (Strings.isNullOrEmpty(apiQuery.getHead())) {
            apiQuery.setHead(getHead());
        }
        return branch.featureService().getFeatures(getName(), apiQuery);
    }

    public void addFeatures(GeogigFeature... features) {
        addFeatures(GeogigFeatureCollection.of(getType(), features));
    }

    public void addFeatures(@NonNull GeogigFeatureCollection features) {
        Preconditions.checkArgument(getName().equals(features.getFeatureType().getName()));
        branch.featureService().addFeatures(features);
    }

    public void modifyFeatures(List<String> attributes, List<Object> values, FeatureFilter filter) {
        Preconditions.checkArgument(attributes.size() == values.size());

        GeogigFeature proto = new GeogigFeature();
        for (int i = 0; i < attributes.size(); i++) {
            String attName = attributes.get(i);
            Object value = values.get(i);
            proto.put(attName, value);
        }

        UpdateFeaturesRequest query = new UpdateFeaturesRequest();
        query.setFilter(filter);
        query.setPrototype(proto);

        String layerName = getName();
        FeatureServiceClient featureService = branch.featureService();
        featureService.modifyFeatures(layerName, query);
    }

    public void removeFeatures(@NonNull FeatureFilter apiFilter) {
        String layerName = getName();
        branch.featureService().removeFeatures(layerName, apiFilter);
    }

    public String getLayerHash() {
        String layerHash = branch.featureService().getLayerHash(getName());
        return layerHash;
    }

    public Optional<SimplePropertyDescriptor> getDefaultGeometry() {
        RevisionFeatureType type = getType();
        String defaultGeometry = type.getDefaultGeometry();
        if (defaultGeometry != null) {
            return getProperty(defaultGeometry);
        }
        return Optional.empty();
    }

    private Optional<SimplePropertyDescriptor> getProperty(@NonNull String propertyName) {
        Stream<SimplePropertyDescriptor> stream = getType().getProperties().stream();
        return stream.filter((p) -> propertyName.equals(p.getName())).findFirst();
    }

    public List<RevisionCommit> getCommits() {
        return branch.getLayerCommits(getName());
    }

    public CompletableFuture<Boolean> delete() {
        return CompletableFuture.supplyAsync(() -> {
            boolean deleted;
            branch.getRepo().startTransaction();
            try {
                branch.checkout();
                deleted = branch.featureService().deleteLayer(getName());
                branch.getRepo().commitSync();
            } catch (RuntimeException e) {
                branch.getRepo().abort();
                throw e;
            }
            return deleted;
        });
    }

    public Iterator<ConflictInfo> getConflicts() {
        return getRepo().getConflicts(getName(), true);
    }

    public long getConflictsCount() {
        return getRepo().getConflictsCount(getName());
    }
}
