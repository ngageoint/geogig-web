package org.geogig.web.client;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.FeatureServiceApi;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.FeatureQuery.ResultTypeEnum;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.UpdateFeaturesRequest;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * A feature service client acts on a single branch for any operation that changes the data, and on
 * that branch for any query method, except that, only for query methods, a different branch can be
 * specified.
 * <p>
 * Also, any operation that changes data on the server will make sure the required branch is already
 * checked out inside the operation's transaction, and if not, it will fail.
 */
public class FeatureServiceClient {

    private @NonNull Supplier<Transaction> transaction;

    private @NonNull Branch branch;

    private FeatureServiceApi api;

    FeatureServiceClient(@NonNull Branch branch, @NonNull Supplier<Transaction> transaction) {
        this.transaction = transaction;
        this.branch = branch;
        this.api = branch.getRepo().client.features;
    }

    public Branch getBranch() {
        return branch;
    }

    public Repo getRepo() {
        return getBranch().getRepo();
    }

    private void checkBranch() throws IllegalStateException {
        final Branch thisBranch = this.branch;
        final Branch currentBranch = getRepo().branches().getCurrentBranch();
        final String thisBranchName = thisBranch.getName();
        final String currentBranchName = currentBranch.getName();
        if (!Objects.equals(thisBranchName, currentBranchName)) {
            UUID txId = transactionOptional();
            String msg = String.format(
                    "Current branch on server is '%s', expected '%s'. Transaction: %s",
                    currentBranchName, thisBranchName, txId);
            throw new IllegalStateException(msg);
        }
    }

    public Layer createLayer(@NonNull RevisionFeatureType type) {
        checkBranch();
        LayerInfo layerInfo;
        try {
            String ownerName = ownerName();
            String repoName = repoName();
            UUID tx = transactionRequired();
            layerInfo = api.createLayer(ownerName, repoName, tx, type);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return new Layer(branch, layerInfo);
    }

    public List<Layer> getLayers() {
        String head = branchName();
        return getLayers(head);
    }

    public List<Layer> getLayers(@NonNull String head) {
        List<LayerInfo> layerInfos;
        try {
            String ownerName = ownerName();
            String repoName = repoName();
            UUID transactionOptional = transactionOptional();
            layerInfos = api.listLayers(ownerName, repoName, transactionOptional, head);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return layerInfos.stream().map((ft) -> new Layer(branch, ft)).collect(Collectors.toList());
    }

    private UUID transactionRequired() {
        UUID tx = transactionOptional();
        if (tx == null) {
            throw new IllegalStateException("No transaction is active");
        }
        return tx;
    }

    private @Nullable UUID transactionOptional() {
        Transaction tx = transaction.get();
        return tx == null ? null : tx.getId();
    }

    public Layer getLayer(@NonNull String layerName) {
        return getLayer(layerName, null);
    }

    public Layer getLayer(@NonNull String layerName, @Nullable Layer cachedVersion)
            throws NoSuchElementException {
        String ifNoneMatch = null;// cachedVersion == null? null:cachedVersion.getInfo().get;
        LayerInfo updatedInfo;
        try {
            String ownerName = ownerName();
            String repoName = repoName();
            String branchName = branchName();
            UUID txId = transactionOptional();
            updatedInfo = api.getLayerInfo(ownerName, repoName, layerName, branchName, txId,
                    ifNoneMatch);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        return updatedInfo == null ? cachedVersion : new Layer(branch, updatedInfo);
    }

    public BoundingBox getBounds(String layer, FeatureQuery apiQuery) {
        if (isAcceptAll(apiQuery.getFilter()) && Strings.isNullOrEmpty(apiQuery.getOldHead())) {
            String head = apiQuery.getHead();
            if (head == null) {
                head = branchName();
            }
            UUID geogigTransactionId = transactionOptional();
            try {
                String user = ownerName();
                String repo = repoName();
                BoundingBox bounds = api.getBounds(user, repo, layer, head, geogigTransactionId);
                return bounds;
            } catch (ApiException e) {
                throw Client.propagate(e);
            }
        }

        apiQuery.setResultType(ResultTypeEnum.BOUNDS);
        apiQuery.setHead(branchName());
        BoundingBox bounds;
        try (GeogigFeatureCollection features = getFeatures(layer, apiQuery)) {
            bounds = features.getBounds()
                    .orElseThrow(() -> new IllegalStateException("Bounds not returned"));
        }
        return bounds;
    }

    public long getSize(@NonNull String layer, @NonNull FeatureQuery apiQuery) {
        if (isAcceptAll(apiQuery.getFilter()) && Strings.isNullOrEmpty(apiQuery.getOldHead())) {
            String head = apiQuery.getHead();
            if (head == null) {
                head = branchName();
            }
            UUID geogigTransactionId = transactionOptional();
            try {
                String user = ownerName();
                String repo = repoName();
                Long size = api.getSize(user, repo, layer, head, geogigTransactionId);
                return size.longValue();
            } catch (ApiException e) {
                throw Client.propagate(e);
            }
        }
        apiQuery.setResultType(ResultTypeEnum.COUNT);
        apiQuery.setHead(branchName());
        Long size;
        try (GeogigFeatureCollection features = getFeatures(layer, apiQuery)) {
            size = features.getSize()
                    .orElseThrow(() -> new IllegalStateException("Size not returned"));
        }
        return size;
    }

    private boolean isAcceptAll(FeatureFilter filter) {
        if (filter == null) {
            return true;
        }
        BoundingBox bbox = filter.getBbox();
        String cqlFilter = filter.getCqlFilter();
        List<String> featureIds = filter.getFeatureIds();
        if (bbox == null && cqlFilter == null && featureIds == null) {
            return true;
        }
        return false;
    }

    public GeogigFeatureCollection getFeatures(@NonNull String layer) {
        return getFeatures(layer, new FeatureQuery());
    }

    public GeogigFeatureCollection getFeatures(@NonNull String layer,
            @NonNull FeatureQuery apiQuery) {

        org.geogig.web.model.FeatureCollection features;
        try {
            String owner = ownerName();
            String repo = repoName();
            UUID txId = transactionOptional();
            if (Strings.isNullOrEmpty(apiQuery.getHead())) {
                apiQuery.setHead(branchName());
            }
            features = api.queryFeatures(owner, repo, layer, apiQuery, txId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
        Preconditions.checkState(features instanceof GeogigFeatureCollection);
        return (GeogigFeatureCollection) features;
    }

    public void addFeatures(@NonNull GeogigFeatureCollection features) {
        checkBranch();
        RevisionFeatureType featureType = features.getFeatureType();
        Preconditions.checkNotNull(featureType, "GeogigFeatureCollection.getFeatureType() is null");
        String owner = ownerName();
        String repoName = repoName();
        String layerName = featureType.getName();
        UUID txId = transactionRequired();

        try {
            api.addFeatures(owner, repoName, layerName, txId, features);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public void modifyFeatures(@NonNull String layerName, @NonNull UpdateFeaturesRequest query) {
        checkBranch();
        Objects.requireNonNull(query.getPrototype());
        Objects.requireNonNull(query.getFilter());

        String owner = ownerName();
        String repoName = repoName();
        UUID txId = transactionRequired();

        try {
            api.modifyFeatures(owner, repoName, layerName, txId, query);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public String getLayerHash(@NonNull String layerName) {
        String ownerName = ownerName();
        String repoName = repoName();
        String head = branchName();
        UUID txId = transactionOptional();
        try {
            return api.getLayerHash(ownerName, repoName, layerName, head, txId);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public void removeFeatures(@NonNull String layerName, @NonNull FeatureFilter filter) {
        checkBranch();
        String ownerName = ownerName();
        String repoName = repoName();
        UUID txId = transactionRequired();
        try {
            api.deleteFeatures(ownerName, repoName, layerName, txId, filter);
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public boolean deleteLayer(@NonNull String layerName) {
        checkBranch();
        String ownerName = ownerName();
        String repoName = repoName();
        UUID txId = transactionRequired();
        try {
            api.deleteLayer(ownerName, repoName, layerName, txId);
            return true;
        } catch (ApiException e) {
            Client.propagateIfNot(e, 404);
            return false;
        }
    }

    private String repoName() {
        return getRepo().getIdentity();
    }

    private String ownerName() {
        return getRepo().getOwnerName();
    }

    private String branchName() {
        return getBranch().getName();
    }

}
