package org.geogig.server.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

public @Value @Builder @Wither class PullRequestStatus {

    public static enum MergeableStatus {
        UNKNOWN, CHECKING, MERGING, MERGEABLE, UNMERGEABLE, MERGED
    }

    private boolean closed, merged;

    private @NonNull PullRequest request;

    private @NonNull MergeableStatus mergeable;

    private long numConflicts;

    private int commitsBehindTargetBranch;

    private int commitsBehindRemoteBranch;

    private @NonNull Optional<String> mergeCommitId;

    private @NonNull String headRef;

    private @NonNull String originRef;

    private @NonNull String mergeRef;

    // private @NonNull Optional<MergeScenarioReport> report;

    private @NonNull List<String> affectedLayers;

    /**
     * Transaction used on the target repo to run the test merge, tha's kept open until the PR is
     * closed
     */
    private UUID transaction;
}
