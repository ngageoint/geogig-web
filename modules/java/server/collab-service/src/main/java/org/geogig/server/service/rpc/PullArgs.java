package org.geogig.server.service.rpc;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class PullArgs {
    private @NonNull UUID targetRepo;

    private @NonNull String targetBranch;

    private @NonNull UUID remoteRepo;

    private @NonNull String remoteBranch;

    private @Nullable String commitMessage;

    private boolean mergeStrategyOurs;

    private boolean mergeStrategyTheirs;

    private boolean noFf;
}
