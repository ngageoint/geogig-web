package org.geogig.server.service.rpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MergeRequest {
    private String base;

    private String head;

    private boolean noFf;

    private String commitMessage;
}
