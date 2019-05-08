package org.geogig.server.service.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.geometry.jts.GeometryClipper;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.RoundingFilter;

class VectorTileGeomPreProcess {

    int TILE_BUFFER = 20;

    int TILE_SIZE = 256;

    Envelope clipEnvelope; // px coordinates

    Envelope tileEnvelope; // world coordinates

    GeometryClipper clipper;

    AffineTransformation xform_world_to_px;

    public VectorTileGeomPreProcess(Envelope tileEnvelope) {
        clipEnvelope = new Envelope(-TILE_BUFFER, TILE_SIZE + TILE_BUFFER, -TILE_BUFFER,
                TILE_SIZE + TILE_BUFFER);
        clipper = new GeometryClipper(clipEnvelope);
        xform_world_to_px = getXform(tileEnvelope);
    }

    // based on wdtinc/mapbox-vector-tile-java and GT VT
    public List<Geometry> processGeom(final Geometry theGeom) {
        // JtsAdapter.flatFeatureList() is inefficient in that it creates a List and a Stack
        // regardless of whether the geom is a collection or not.

        Geometry jtsGeom = theGeom;
        if (jtsGeom.getNumGeometries() == 1) {// circumvent single child collections
            jtsGeom = jtsGeom.getGeometryN(0);
            jtsGeom.setUserData(theGeom.getUserData());
        }
        // and only call flatFeatureList() if strictly necessary
        final List<Geometry> flat = jtsGeom instanceof GeometryCollection
                ? JtsAdapter.flatFeatureList(jtsGeom)
                : Collections.singletonList(jtsGeom);

        final List<Geometry> finalGeoms = new ArrayList<>(jtsGeom.getNumGeometries());
        for (Geometry geom : flat) {
            Object userData = geom.getUserData();

            Geometry geom_xform = xform_world_to_px.transform(geom); // to screen coords
            // generalize
            geom_xform = DouglasPeuckerSimplifier.simplify(geom_xform, 0.25);
            if (geom_xform.isEmpty())
                continue;
            // make coordinates integers
            geom_xform.apply(RoundingFilter.INSTANCE);
            if (geom_xform.isEmpty())
                continue;
            // clip - use non-robust
            geom_xform = clipper.clip(geom_xform, false);
            if (geom_xform == null)
                continue;
            geom_xform.setUserData(userData);
            finalGeoms.add(geom_xform);
        }

        return finalGeoms;
    }

    // based on wdtinc/mapbox-vector-tile-java
    private AffineTransformation getXform(Envelope tileEnvelope) {
        AffineTransformation tx = new AffineTransformation();

        // px (0,0) is the LL corner of tile
        tx.translate(-tileEnvelope.getMinX(), -tileEnvelope.getMinY());

        // simple scale (flip y)
        // this will have y going from 0 -> -255

        tx.scale(1d / (tileEnvelope.getWidth() / (double) TILE_SIZE),
                -1d / (tileEnvelope.getHeight() / (double) TILE_SIZE));

        // because the y is fliped, we move it back to the origin
        // (255,255) should be TR of tile
        tx.translate(0d, (double) TILE_SIZE);

        return tx;
    }
}
