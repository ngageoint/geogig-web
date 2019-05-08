package org.geogig.server.service.branch;

import java.util.List;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.StoredDiffSummary;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

public @Value @Builder class DiffSummary {

    private final @NonNull RepoInfo leftRepo, rightRepo;

    private final List<StoredDiffSummary> layerDiffSummary;
}
