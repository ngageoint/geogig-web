package org.geogig.server.app.gateway;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.service.feature.FeatureService;
import org.geogig.server.service.feature.VectorTileService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerInfo;
import org.geogig.web.model.LayerSummary;
import org.geogig.web.model.RepoLayerSummary;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.UpdateFeaturesRequest;
import org.geogig.web.model.UserLayerSummary;
import org.geogig.web.server.api.FeatureServiceApi;
import org.geogig.web.server.api.FeatureServiceApiDelegate;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;

import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link FeatureServiceApi},
 * handles the REST request/response/error handling aspects of the API, and delegates business logic
 * to a {@link FeatureService}.
 */
public @Service class FeaturesServiceApi extends AbstractService
        implements FeatureServiceApiDelegate {

    private final RepositoryManagementService repositories;

    private final FeatureService layers;

    private final VectorTileService tiles;

    public @Autowired FeaturesServiceApi(//@formatter:off
            FeatureService service,
            RepositoryManagementService repositories,
            VectorTileService tiles) {//@formatter:on
        this.layers = service;
        this.repositories = repositories;
        this.tiles = tiles;
    }

    public @Override ResponseEntity<List<UserLayerSummary>> getLayersSummaries() {
        // List<RepoInfo> allrepos = repositories.getAll();
        // ConcurrentMap<String, UserLayerSummary> byUser = new ConcurrentHashMap<>();
        // for (RepoInfo repo : allrepos) {
        // String user = repo.getOwner().getIdentity();
        //
        // UserLayerSummary summary;
        // summary = byUser.computeIfAbsent(user, (u) -> new UserLayerSummary().userName(user));
        //
        // RepoLayerSummary repoLayers = toRepoLayerSummary(repo);
        // summary.addRepositoriesItem(repoLayers);
        // }
        // List<UserLayerSummary> all = new ArrayList<>();
        // all.addAll(byUser.values());
        // Collections.sort(all, (s1, s2) -> s1.getUserName().compareTo(s2.getUserName()));
        // return super.ok(all);
        throw new UnsupportedOperationException();
    }

    public @Override ResponseEntity<List<RepoLayerSummary>> getUserLayersSummaries(String user) {
        // List<RepositoryInfo> userRepos = repositories.getByUser(user);
        // List<RepoLayerSummary> userLayersSummary = userRepos.stream()
        // .map((r) -> toRepoLayerSummary(r)).collect(Collectors.toList());
        // return super.ok(userLayersSummary);
        throw new UnsupportedOperationException();
    }

    private RepoLayerSummary toRepoLayerSummary(RepositoryInfo repo) {
        RepoLayerSummary rs = new RepoLayerSummary().repository(repo.getIdentity());
        List<LayerInfo> repoLayers = layers.getLayers(repo.getOwner().getIdentity(),
                repo.getIdentity(), null, null);
        for (LayerInfo li : repoLayers) {
            LayerSummary ls = new LayerSummary().name(li.getName()).title(li.getTitle())
                    ._abstract(li.getAbstract());
            rs.addLayersItem(ls);
        }
        return rs;
    }

    public @Override ResponseEntity<LayerInfo> createLayer(String user, String repo,
            UUID geogigTransactionId, RevisionFeatureType schema) {

        LayerInfo newLayerInfo;
        try {
            newLayerInfo = layers.create(user, repo, geogigTransactionId, schema);
        } catch (IllegalArgumentException iae) {
            return badRequest(iae.getMessage());
        } catch (NoSuchElementException nse) {
            return notFound(nse.getMessage());
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }

        return created(newLayerInfo);
    }

    public @Override ResponseEntity<List<LayerInfo>> listLayers(String user, String repo,
            UUID geogigTransactionId, String head) {

        return super.ok(layers.getLayers(user, repo, geogigTransactionId, head));
    }

    public @Override ResponseEntity<Void> deleteLayer(String user, String repo, String layer,
            UUID txId) {

        return super.run(HttpStatus.NO_CONTENT, () -> layers.deleteLayer(user, repo, layer, txId));
    }

    //@formatter:off
    public @Override ResponseEntity<org.geogig.web.model.Feature> getFeature(
            String user, 
            String repo, 
            String layer,
            String featureId, 
            String head, 
            UUID geogigTransactionId, 
            String ifNoneMatch) {
        //@formatter:on

        FeatureQuery query = new FeatureQuery()
                .filter(new FeatureFilter().addFeatureIdsItem(featureId));

        if (!Strings.isNullOrEmpty(head)) {
            query.setHead(head);
        }

        GeogigFeatureCollection features;
        features = layers.getFeatures(user, repo, layer, query, geogigTransactionId);

        if (features.hasNext()) {
            GeogigFeature next = features.next();
            return ok(next);
        }
        return super.notFound();
    }

    //@formatter:off
    public @Override ResponseEntity<org.geogig.web.model.FeatureCollection> getFeatures(
            String user,
            String repo,
            String layer,
            List<Double> bbox,
            List<String> attributes,
            String head,
            UUID geogigTransactionId,
            Integer page,
            Integer pageSize,
            String ifNoneMatch) {
    //@formatter:on

        FeatureQuery query = new FeatureQuery().filter(new FeatureFilter());
        if (bbox != null && !bbox.isEmpty()) {
            BoundingBox boundingBox = new BoundingBox();
            boundingBox.addAll(bbox);
            query.getFilter().setBbox(boundingBox);
        }
        if (attributes != null) {
            query.setAttributes(attributes);
        }
        if (!Strings.isNullOrEmpty(head)) {
            query.setHead(head);
        }
        if (page != null || pageSize != null) {
            if (pageSize != null) {
                if (page == null) {
                    return super.badRequest("Requested pageSize=%d but page parameter not provided",
                            pageSize);
                }
            }
            if (page != null && pageSize == null) {
                pageSize = 100;
            }
            Integer limit = pageSize == null ? null : pageSize;
            Integer offset = page == null ? null : page * pageSize;
            query.offset(offset).limit(limit);
        }

        return queryFeatures(user, repo, layer, query, geogigTransactionId);
    }

    //@formatter:off
    public @Override ResponseEntity<FeatureCollection> queryFeatures(
            String user,
            String repo,
            String layer,
            FeatureQuery query,
            UUID geogigTransactionId) {
      //@formatter:on

        GeogigFeatureCollection collection;
        collection = layers.getFeatures(user, repo, layer, query, geogigTransactionId);
        Map<String, String> headers = collection.getExtraData();
        return super.run(HttpStatus.OK, () -> collection, headers);
    }

    /**
     * @see FeatureServiceApi#getDiffTile
     */
    public @Override ResponseEntity<byte[]> getDiffTile(String user, String repo, String layer,
            Integer z, Integer x, Integer y, String revA, String revB, List<String> attributes,
            String head, UUID geogigTransactionId, String ifNoneMatch) {
        final UUID repoId = repositories.resolveId(user, repo);
        int tileX = x.intValue();
        int tileY = y.intValue();
        int tileZ = z.intValue();

        CompletableFuture<Tile> tileFuture;
        try {
            tileFuture = tiles.getDiffTile(repoId, layer, tileX, tileY, tileZ, revA, revB,
                    geogigTransactionId, attributes);
        } catch (NoSuchElementException notFound) {
            return super.notFound();
        } catch (ArrayIndexOutOfBoundsException badTile) {
            return super.notFound("Tile for %s:%s:%s/%d/%d/%d is outside layer bounds", user, repo,
                    layer, z, x, y);
        }
        Tile tile = tileFuture.join();
        return super.ok(tile.toByteArray());
    }

    //@formatter:off
    public @Override ResponseEntity<byte[]> getTile(
            @NonNull String user,
            @NonNull String repo,
            @NonNull String layer,
            @NonNull Integer z,
            @NonNull Integer x,
            @NonNull Integer y,
            @Nullable List<String> attributes,
            @Nullable String head,
            @Nullable UUID txId,
            @Nullable String ifNoneMatch) {
        //@formatter:on

        final UUID repoId = repositories.resolveId(user, repo);
        int tileX = x.intValue();
        int tileY = y.intValue();
        int tileZ = z.intValue();

        CompletableFuture<Tile> tileFuture;
        try {
            tileFuture = tiles.getTile(repoId, layer, tileX, tileY, tileZ, txId, attributes);
        } catch (NoSuchElementException notFound) {
            return super.notFound();
        } catch (ArrayIndexOutOfBoundsException badTile) {
            return super.notFound("Tile for %s:%s:%s/%d/%d/%d is outside layer bounds", user, repo,
                    layer, z, x, y);
        }
        Tile tile = tileFuture.join();
        return super.ok(tile.toByteArray());
    }

    //@formatter:off
    public @Override ResponseEntity<String> getLayerHash(
            String user, 
            String repo, 
            String layer,
            String head, 
            UUID geogigTransactionId) {
        //@formatter:on
        if (getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {
                    return new ResponseEntity<>(
                            getObjectMapper().get().readValue("{ }", String.class),
                            HttpStatus.NOT_IMPLEMENTED);
                } catch (IOException e) {
                    log.error("Couldn't serialize response for content type application/json", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            log.warn(
                    "ObjectMapper or HttpServletRequest not configured in default LayersApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    //@formatter:off
    public @Override ResponseEntity<LayerInfo> getLayerInfo(
            String user, 
            String repo, 
            String layer,
            String head, 
            UUID geogigTransactionId, 
            String ifNoneMatch) {
        //@formatter:on

        return run(HttpStatus.OK,
                () -> layers.getLayer(user, repo, layer, head, geogigTransactionId));
    }

    //@formatter:off
    public @Override ResponseEntity<RevisionFeatureType> getSchema(
            String user,
            String repo,
            String layer,
            String head,
            UUID geogigTransactionId) {
        //@formatter:on

        return ok(layers.getLayer(user, repo, layer, head, geogigTransactionId).getType());
    }

    //@formatter:off
    public @Override ResponseEntity<Long> getSize(
            String user,
            String repo,
            String layer,
            String head,
            UUID geogigTransactionId) {
        //@formatter:on

        return ok(layers.getLayer(user, repo, layer, head, geogigTransactionId).getSize());
    }

    //@formatter:off
    public @Override ResponseEntity<BoundingBox> getBounds(String user,
            String repo,
            String layer,
            String head,
            UUID geogigTransactionId) {
        //@formatter:on

        return ok(layers.getLayer(user, repo, layer, head, geogigTransactionId).getBounds());
    }

    //@formatter:off
    public @Override ResponseEntity<Void> addFeatures(String user,
            String repo,
            String layer,
            UUID txId,
            org.geogig.web.model.Feature featureOrCollection) {
        //@formatter:on

        GeogigFeatureCollection featureCollection;
        if ("Feature".equals(featureOrCollection.getType())) {
            GeogigFeature feature = (GeogigFeature) featureOrCollection;
            featureCollection = GeogigFeatureCollection.of(feature);
        } else {
            featureCollection = (GeogigFeatureCollection) featureOrCollection;
        }

        return super.run(HttpStatus.NO_CONTENT,
                () -> layers.insert(user, repo, layer, featureCollection, txId));
    }

    //@formatter:off
    public @Override ResponseEntity<Void> modifyFeatures(
            String user,
            String repo,
            String layer,
            UUID txId,
            UpdateFeaturesRequest query) {
        //@formatter:on

        return super.run(HttpStatus.NO_CONTENT,
                () -> layers.update(user, repo, layer, txId, query));
    }

    //@formatter:off
    public @Override ResponseEntity<Void> truncate(
            String user,
            String repo,
            String layer,
            UUID geogigTransactionId) {
        //@formatter:on

        FeatureFilter filter = new FeatureFilter();
        filter.setCqlFilter("INCLUDE");
        return deleteFeatures(user, repo, layer, geogigTransactionId, filter);
    }

    //@formatter:off
    public @Override ResponseEntity<Void> deleteFeatures(
            String user,
            String repo,
            String layer,
            UUID txId,
            FeatureFilter filter) {
        //@formatter:on

        return super.run(HttpStatus.NO_CONTENT,
                () -> layers.delete(user, repo, layer, txId, filter));
    }

    //@formatter:off
    public @Override ResponseEntity<Void> deleteFeature(
            String user, 
            String repo, 
            String layer,
            String featureId, 
            UUID geogigTransactionId) {
        //@formatter:on
        if (getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
        } else {
            log.warn(
                    "ObjectMapper or HttpServletRequest not configured in default LayersApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    //@formatter:off
    public @Override ResponseEntity<Void> modifyFeature(
            String user, 
            String repo, 
            String layer,
            String featureId, 
            UUID geogigTransactionId) {
        //@formatter:on

        if (getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
        } else {
            log.warn(
                    "ObjectMapper or HttpServletRequest not configured in default LayersApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
