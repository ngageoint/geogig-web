package org.geogig.server.service.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.web.model.FeatureFilter;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.FeatureQuery.ResultTypeEnum;
import org.geogig.web.model.GeogigFeature;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.RevisionFeatureType;
import org.geogig.web.model.SRS;
import org.geogig.web.model.SimplePropertyDescriptor;
import org.geogig.web.model.ValueType;
import org.geogig.web.model.ValueTypes;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("VectorTileService")
public class VectorTileService {

    private @Autowired FeatureService featureService;

    @Async
    public CompletableFuture<VectorTile.Tile> getDiffTile(//@formatter:off
                                                      @NonNull UUID repoId,
                                                      @NonNull String layer,
                                                      final int tileX,
                                                      final int tileY,
                                                      final int tileZ,
                                                      final String revA,
                                                      final String revB,
                                                      @Nullable UUID txId,
                                                      @Nullable List<String> attributes) {
        //@formatter:on

        FeatureQuery query = new FeatureQuery();
        query.setResultType(ResultTypeEnum.FEATURES);
        query.setAttributes(attributes);
        query.setScreenWidth(256);
        query.setScreenHeight(256);
        query.setHead(revB);
        query.setOldHead(revA);
        query.setFlattenDiffSchema(true);

        // query.setOutputCrs(new SRS().authorityCode("EPSG:3857"));

        final Envelope tileBounds = tileBounds(tileX, tileY, tileZ);
        FeatureFilter tileBoundsFilter = createTileFilter(tileBounds);
        query.setFilter(tileBoundsFilter);

        try (GeogigFeatureCollection features = featureService.getFeatures(repoId, txId, layer,
                query)) {
            VectorTile.Tile tile;
            tile = createDiffTile(features, tileBounds);
            return CompletableFuture.completedFuture(tile);
        }
    }

    @Async
    public CompletableFuture<VectorTile.Tile> getTile(//@formatter:off
            @NonNull UUID repoId, 
            @NonNull String layer,
            final int tileX,
            final int tileY,
            final int tileZ,
            @Nullable UUID txId, 
            @Nullable List<String> attributes) {
        //@formatter:on

        FeatureQuery query = new FeatureQuery();
        query.setResultType(ResultTypeEnum.FEATURES);
        query.setAttributes(attributes);
        query.setScreenWidth(256);
        query.setScreenHeight(256);
        query.setHead("master");

        query.setOutputCrs(new SRS().authorityCode("EPSG:3857"));

        final Envelope tileBounds = tileBounds(tileX, tileY, tileZ);
        FeatureFilter tileBoundsFilter = createTileFilter(tileBounds);
        query.setFilter(tileBoundsFilter);

        try (GeogigFeatureCollection features = featureService.getFeatures(repoId, txId, layer,
                query)) {
            VectorTile.Tile tile;
            tile = createTile(features, tileBounds);
            return CompletableFuture.completedFuture(tile);
        }
    }

    private VectorTile.Tile createDiffTile(@NonNull GeogigFeatureCollection features,
            @NonNull Envelope tileEnvelope) {

        // final IGeometryFilter acceptAllGeomFilter = geometry -> true;
        final RevisionFeatureType featureType = features.getFeatureType();
        Preconditions.checkNotNull(featureType);

        final String layerName = featureType.getName();
        // final String defaultGeometry = featureType.getDefaultGeometry();
        // final List<String> featureAttributes = encodeableAttributes(featureType);

        final MvtLayerParams layerParams = new MvtLayerParams(256, 256); // don't over sample
        final List<String> featureAtributes = encodeableAttributes(featureType);

        final IUserDataConverter userDataConverter = new GeogigFeatureUserDataConverter(
                featureAtributes);
        VectorTile.Tile.Layer.Builder layerBuilder_add = MvtLayerBuild
                .newLayerBuilder(layerName + ".add", layerParams);
        VectorTile.Tile.Layer.Builder layerBuilder_delete = MvtLayerBuild
                .newLayerBuilder(layerName + ".delete", layerParams);
        VectorTile.Tile.Layer.Builder layerBuilder_change = MvtLayerBuild
                .newLayerBuilder(layerName + ".change", layerParams);

        MvtLayerProps props_add = new MvtLayerProps();
        MvtLayerProps props_delete = new MvtLayerProps();
        MvtLayerProps props_change = new MvtLayerProps();

        VectorTileGeomPreProcess geomPreProcessor = new VectorTileGeomPreProcess(tileEnvelope);
        while (features.hasNext()) {
            GeogigFeature feature = features.next();

            if (((Integer) feature.get("geogig.changeType")).intValue() == 0) {// add
                Geometry jtsGeom = (Geometry) feature.get("new_way");
                if (jtsGeom == null)
                    continue;
                jtsGeom.setUserData(feature);
                try {
                    List<Geometry> tileGeom = geomPreProcessor.processGeom(jtsGeom);

                    List<VectorTile.Tile.Feature> vtfeatures = JtsAdapter.toFeatures(tileGeom,
                            props_add, userDataConverter);

                    layerBuilder_add.addAllFeatures(vtfeatures);
                } catch (Exception te) {
                    System.out.println("Exception encoding " + feature.getId() + " type=" + te);
                    // te.printStackTrace();
                }
            }
            if (((Integer) feature.get("geogig.changeType")).intValue() == 2) {// delete
                Geometry jtsGeom = (Geometry) feature.get("old_way");
                if (jtsGeom == null)
                    continue;
                jtsGeom.setUserData(feature);
                try {
                    List<Geometry> tileGeom = geomPreProcessor.processGeom(jtsGeom);

                    List<VectorTile.Tile.Feature> vtfeatures = JtsAdapter.toFeatures(tileGeom,
                            props_delete, userDataConverter);

                    layerBuilder_delete.addAllFeatures(vtfeatures);
                } catch (Exception te) {
                    System.out.println("Exception encoding " + feature.getId() + " type=" + te);
                    // te.printStackTrace();
                }
            }
            if (((Integer) feature.get("geogig.changeType")).intValue() == 1) {// update
                Geometry jtsGeom = (Geometry) feature.get("new_way");
                if (jtsGeom == null)
                    continue;
                jtsGeom.setUserData(feature);
                try {
                    List<Geometry> tileGeom = geomPreProcessor.processGeom(jtsGeom);

                    List<VectorTile.Tile.Feature> vtfeatures = JtsAdapter.toFeatures(tileGeom,
                            props_change, userDataConverter);

                    layerBuilder_change.addAllFeatures(vtfeatures);
                } catch (Exception te) {
                    System.out.println("Exception encoding " + feature.getId() + " type=" + te);
                    // te.printStackTrace();
                }
            }
        }

        MvtLayerBuild.writeProps(layerBuilder_add, props_add);
        MvtLayerBuild.writeProps(layerBuilder_delete, props_delete);
        MvtLayerBuild.writeProps(layerBuilder_change, props_change);

        VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

        tileBuilder.addLayers(layerBuilder_add.build());
        tileBuilder.addLayers(layerBuilder_delete.build());
        tileBuilder.addLayers(layerBuilder_change.build());

        VectorTile.Tile mvt = tileBuilder.build();
        return mvt;
    }

    private VectorTile.Tile createTile(@NonNull GeogigFeatureCollection features,
            @NonNull Envelope tileEnvelope) {

        final RevisionFeatureType featureType = features.getFeatureType();
        Preconditions.checkNotNull(featureType);

        final String layerName = featureType.getName();
        final String defaultGeometry = featureType.getDefaultGeometry();
        final List<String> featureAtributes = encodeableAttributes(featureType);
        Preconditions.checkArgument(defaultGeometry != null, "Layer %s has no geometry attribute");

        final MvtLayerParams layerParams = new MvtLayerParams(256, 256); // don't over sample
        final IUserDataConverter userDataConverter = new GeogigFeatureUserDataConverter(
                featureAtributes);

        VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName,
                layerParams);

        MvtLayerProps props = new MvtLayerProps();

        VectorTileGeomPreProcess geomPreProcessor = new VectorTileGeomPreProcess(tileEnvelope);
        while (features.hasNext()) {
            GeogigFeature feature = features.next();
            Geometry jtsGeom = (Geometry) feature.get(defaultGeometry);
            if (jtsGeom == null) {
                continue;
            }
            jtsGeom.setUserData(feature);
            List<Geometry> tileGeom;
            try {
                tileGeom = geomPreProcessor.processGeom(jtsGeom);

                List<VectorTile.Tile.Feature> vtfeatures = JtsAdapter.toFeatures(tileGeom, props,
                        userDataConverter);

                layerBuilder.addAllFeatures(vtfeatures);
            } catch (Exception te) {
                log.warn("Exception encoding {}", feature.getId(), te);
            }
        }

        MvtLayerBuild.writeProps(layerBuilder, props);
        VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

        tileBuilder.addLayers(layerBuilder.build());
        VectorTile.Tile mvt = tileBuilder.build();
        return mvt;
    }

    private List<String> encodeableAttributes(RevisionFeatureType featureType) {
        List<SimplePropertyDescriptor> props = featureType.getProperties();
        List<String> encodeable = new ArrayList<>(props.size());
        for (SimplePropertyDescriptor p : props) {
            ValueType valueType = p.getBinding();
            Class<?> javaType = ValueTypes.getJavaTypeBinding(valueType);
            if (Geometry.class.isAssignableFrom(javaType)) {
                continue;
            }
            if (javaType.isArray() || Math.class.isAssignableFrom(javaType)
                    || Collection.class.isAssignableFrom(javaType)) {
                continue;
            }
            encodeable.add(p.getName());
        }
        return encodeable;
    }

    private FeatureFilter createTileFilter(final Envelope bounds) {
        FeatureFilter f = new FeatureFilter();
        f.setBbox(GeoToolsDomainBridge.toBounds(bounds));
        return f;
    }

    /**
     * <pre>
     * {@code
     * minx, miny = self.PixelsToMeters( tx*self.tileSize, ty*self.tileSize, zoom ) 
     * maxx, maxy = self.PixelsToMeters( (tx+1)*self.tileSize, (ty+1)*self.tileSize, zoom )
     * }
     * </pre>
     */
    private Envelope tileBounds(final int tx, final int ty, final int zoom) {
        double[] min = pixelsToMeters(tx * tileSize, ty * tileSize, zoom);
        double[] max = pixelsToMeters((tx + 1) * tileSize, (ty + 1) * tileSize, zoom);
        return new Envelope(min[0], max[0], min[1], max[1]);
    }

    /**
     * <pre>
     * {@code
     * res = self.Resolution( zoom ) 
     * mx = px * res - self.originShift 
     * my = py * res - self.originShift 
     * return mx, my
     * }
     * </pre>
     */
    private double[] pixelsToMeters(double px, double py, int zoom) {
        double res = resolution(zoom);
        double mx = px * res - originShift;
        double my = py * res - originShift;
        return new double[] { mx, my };
    }

    private final double resolution(int zoom) {
        return initialResolution / Math.pow(2, zoom);
    }

    private static final double tileSize = 256;

    private static final double initialResolution = 2 * Math.PI * 6378137 / tileSize;

    // # 156543.03392804062 for tileSize 256 pixels
    private static final double originShift = 2 * Math.PI * 6378137 / 2.0;

}
