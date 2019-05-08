package org.geogig.server.service.async;

import lombok.Data;
import lombok.NoArgsConstructor;

public @Data @NoArgsConstructor class AsyncTaskProgress {
    private String taskDescription;

    private String progressDescription;

    private double maxProgress = -1.0d;

    private double progress = -1.0d;

}
