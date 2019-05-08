package org.geogig.server.service.branch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.geogig.server.model.StoredDiffSummary;
import org.locationtech.geogig.model.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import lombok.NonNull;

public @Service class LayerDiffSummaryJPAStore implements LayerDiffSummaryStore {

    private @Autowired LayerDiffSummaryRepository repo;

    public @Override Optional<List<StoredDiffSummary>> find(@NonNull ObjectId left,
            @NonNull ObjectId right) {

        final String lkey = left.toString();
        final String rkey = right.toString();
        boolean exists = repo.existsByLeftRootIdAndRightRootId(lkey, rkey);
        if (!exists) {
            return Optional.empty();
        }
        Iterable<StoredDiffSummary> layerdiffreports = repo.findByLeftRootIdAndRightRootId(lkey,
                rkey);
        ArrayList<StoredDiffSummary> diffs = Lists.newArrayList(layerdiffreports);
        return Optional.ofNullable(diffs);
    }

    public @Override void save(List<StoredDiffSummary> result) {
        repo.saveAll(result);
    }
}
