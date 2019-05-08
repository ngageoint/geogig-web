package org.geogig.server.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

public @Data @NoArgsConstructor @AllArgsConstructor class StoredDiffSummaryKey
        implements Serializable {

    private static final long serialVersionUID = 7637755115600318387L;

    private @NonNull String leftRootId;

    private @NonNull String rightRootId;

    private @NonNull String path;
}
