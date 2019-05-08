package org.geogig.server.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@IdClass(StoredDiffSummaryKey.class)
public @Entity @Data @Builder @NoArgsConstructor @AllArgsConstructor class StoredDiffSummary {

    public static @Data @NoArgsConstructor @AllArgsConstructor @Embeddable class Bounds {
        private double minX, minY, maxX, maxY;

    }

    private @NonNull @Id String leftRootId;

    private @NonNull @Id String rightRootId;

    private @NonNull @Id String path;

    private @NonNull @Column(nullable = false) String leftPathTree;

    private @NonNull @Column(nullable = false) String rightPathTree;

    private long featuresAdded, featuresRemoved, featuresChanged;

    @AttributeOverrides({ //
            @AttributeOverride(name = "minX", column = @Column(name = "minx_left")),
            @AttributeOverride(name = "minY", column = @Column(name = "miny_left")),
            @AttributeOverride(name = "maxX", column = @Column(name = "maxx_left")),
            @AttributeOverride(name = "maxY", column = @Column(name = "maxy_left"))//
    })
    private @Embedded @Column(nullable = true) Bounds leftBounds;

    @AttributeOverrides({ //
            @AttributeOverride(name = "minX", column = @Column(name = "minx_right")),
            @AttributeOverride(name = "minY", column = @Column(name = "miny_right")),
            @AttributeOverride(name = "maxX", column = @Column(name = "maxx_right")),
            @AttributeOverride(name = "maxY", column = @Column(name = "maxy_right"))//
    })
    private @Embedded @Column(nullable = true) Bounds rightBounds;
}
