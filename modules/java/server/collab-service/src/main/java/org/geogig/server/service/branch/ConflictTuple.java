package org.geogig.server.service.branch;

import java.util.List;

import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.repository.Conflict;

import lombok.AllArgsConstructor;
import lombok.Value;

public @Value @AllArgsConstructor class ConflictTuple {

    private final Conflict conflict;

    private RevFeature ancestor, theirs, ours;

    private List<String> conflictAttributes;

    public ConflictTuple(Conflict conflict) {
        this.conflict = conflict;
        this.ancestor = null;
        this.theirs = null;
        this.ours = null;
        this.conflictAttributes = null;
    }
}