package org.geogig.server.service.branch;

import java.util.Optional;

import javax.annotation.Nullable;

import org.geogig.server.model.RepoInfo;
import org.locationtech.geogig.model.RevCommit;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class Branch {

    private @NonNull String name;

    private @Nullable String description;

    private @Nullable RevCommit commit;

    private @NonNull Optional<String> remoteName, remoteBranch;

    private @NonNull Optional<RepoInfo> remote;

    private @NonNull Optional<Integer> commitsBehind, commitsAhead;
}
