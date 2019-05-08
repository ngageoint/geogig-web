package org.geogig.web.client.datastore;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.client.Branch;
import org.geogig.web.client.Client;
import org.geogig.web.client.FeatureServiceClient;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeogigWebDatastore extends ContentDataStore {

    private Repo repoClient;

    private Branch branch;

    private @Nullable String headRefSpec;

    public GeogigWebDatastore(@NonNull Repo repoClient) {
        this.repoClient = repoClient;
        this.branch = repoClient.branches().getCurrentBranch();
    }

    public GeogigWebDatastore(@NonNull Branch branch) {
        this.repoClient = branch.getRepo();
        this.branch = branch;
    }

    public void setHeadRefSpec(final String treeIsh) {
        this.headRefSpec = treeIsh;
    }

    public Repo getRepo() {
        return repoClient;
    }

    public Branch getBranch() {
        return branch;
    }

    public Client getClient() {
        return repoClient.getClient();
    }

    public Repo startTransaction() {
        return repoClient.clone().startTransaction();
    }

    protected @Override List<Name> createTypeNames() throws IOException {
        Branch branch = getBranch();
        FeatureServiceClient featureService = branch.featureService();
        List<Layer> layers = featureService.getLayers();
        List<Name> names = layers.stream()
                .map((l) -> new NameImpl(getNamespaceURI(), l.getInfo().getName()))
                .collect(Collectors.toList());
        return names;
    }

    protected @Override ContentFeatureSource createFeatureSource(ContentEntry entry)
            throws IOException {
        if (this.headRefSpec != null) {// read only
            return new GeogigWebFeatureSource(entry, headRefSpec);
        }
        return new GeogigWebFeatureStore(entry);
    }

    public GeogigWebDiffFeatureSource getDiffFeatureSource(@NonNull String layerName)
            throws IOException {

        final @Nullable String namespaceURI = getNamespaceURI();
        Name typeName = new NameImpl(namespaceURI, layerName);

        ensureEntry(typeName);// make sure the featuretype exists

        // can't reuse the content entry as it has the native schema cached, and the diff feature
        // source creates its own FeatureType with "old" and "new" SimpleFeature attributes
        final ContentEntry entry = new ContentEntry(this, typeName);
        GeogigWebDiffFeatureSource source = new GeogigWebDiffFeatureSource(entry);
        return source;
    }

    public @Override void createSchema(@NonNull SimpleFeatureType featureType) throws IOException {
        final boolean manageTransaction = !branch.getRepo().isTransactionPresent();
        Repo repo = getRepo();
        Branch branch = getBranch();
        RevisionFeatureType type = GeoToolsDomainBridge.toFeatureType(featureType);
        try {
            if (manageTransaction) {
                repo = branch.getRepo().startTransaction();
                branch = repo.branches().get(branch.getName());
            }
            branch.checkout();
            Layer layer = branch.featureService().createLayer(type);
            log.debug("Created layer {}:{}:{}:{}", layer.getRepo().getOwnerName(),
                    layer.getRepo().getIdentity(), layer.getBranch().getName(), layer.getName());

            if (manageTransaction) {
                TransactionInfo result = repo.commitSync("Create FeatureType " + type.getName(),
                        null);
                Preconditions.checkNotNull(result);
                TransactionStatus status = result.getStatus();
                if (!TransactionStatus.COMMITTED.equals(status)) {
                    throw new IOException("Transaction failed: " + status + ": " + result);
                }
            }
        } catch (RuntimeException e) {
            Throwable cause = Throwables.getRootCause(e);
            throw new IOException("Error creating feature type " + featureType.getName(), cause);
        }
    }

    public @Override void removeSchema(@NonNull String typeName) throws IOException {
        Repo repo = getRepo();
        Branch branch = getBranch();
        final boolean manageTransaction = !repo.isTransactionPresent();
        try {
            if (manageTransaction) {
                repo = repo.clone().startTransaction();
                branch = repo.branches().get(branch.getName());
            }
            branch.checkout();
            repo.deleteLayer(typeName);
            if (manageTransaction) {
                TransactionInfo result = repo.commitSync("Delete FeatureType " + typeName, null);
                Preconditions.checkNotNull(result);
                TransactionStatus status = result.getStatus();
                if (!TransactionStatus.COMMITTED.equals(status)) {
                    throw new IOException("Transaction failed: " + status + ": " + result);
                }
            }
        } catch (RuntimeException e) {
            Throwable cause = Throwables.getRootCause(e);
            throw new IOException("Error deleting feature type " + typeName, cause);
        }
    }
}
