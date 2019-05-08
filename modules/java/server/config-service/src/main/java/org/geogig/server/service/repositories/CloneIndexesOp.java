package org.geogig.server.service.repositories;

import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.storage.IndexDatabase;

import lombok.NonNull;

public class CloneIndexesOp extends AbstractGeoGigOp<Void> {

    private Repository origin;

    public CloneIndexesOp setOrigin(@NonNull Repository origin) {
        this.origin = origin;
        return this;
    }

    protected @Override Void _call() {
        ProgressListener progress = getProgressListener();
        progress.setProgressIndicator(p -> "Cloning spatial indexes...");
        IndexDatabase myIndexes = indexDatabase();
        IndexDatabase originIdexes = origin.indexDatabase();
        originIdexes.copyIndexesTo(myIndexes);
        return null;
    }

}
