package org.geogig.server.service.branch;

import org.geogig.server.model.StoredDiffSummary;
import org.geogig.server.model.StoredDiffSummaryKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public @Repository interface LayerDiffSummaryRepository
        extends CrudRepository<StoredDiffSummary, StoredDiffSummaryKey> {

    public Iterable<StoredDiffSummary> findByLeftRootIdAndRightRootId(String leftId,
            String rightId);

    public boolean existsByLeftRootIdAndRightRootId(String leftId, String rightId);
}
