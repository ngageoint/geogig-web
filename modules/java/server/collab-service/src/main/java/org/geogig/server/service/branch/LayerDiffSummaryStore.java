package org.geogig.server.service.branch;

import java.util.List;
import java.util.Optional;

import org.geogig.server.model.StoredDiffSummary;
import org.locationtech.geogig.model.ObjectId;
import org.springframework.stereotype.Service;

public @Service interface LayerDiffSummaryStore {

    Optional<List<StoredDiffSummary>> find(ObjectId left, ObjectId right);

    void save(List<StoredDiffSummary> result);
}
