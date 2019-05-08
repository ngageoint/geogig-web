package org.geogig.server.service.pr;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Wither;

@Data
@Builder
@Wither
@AllArgsConstructor
public class PullRequestRequest implements Cloneable {

    private final @NonNull UUID issuerUser;

    private final @NonNull UUID issuerRepo;

    private final @NonNull String issuerBranch;

    private final @NonNull UUID targetRepo;

    private final @NonNull String targetBranch;

    private @NonNull String title;

    private @Nullable String description;

    PullRequestRequest() {
        issuerUser = null;
        issuerRepo = null;
        issuerBranch = null;
        targetRepo = null;
        targetBranch = null;
        title = null;
        description = null;
    }

    public @Override PullRequestRequest clone() {
        try {
            return (PullRequestRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
