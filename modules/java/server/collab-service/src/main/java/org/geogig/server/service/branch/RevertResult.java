package org.geogig.server.service.branch;

import java.util.UUID;

import org.locationtech.geogig.model.ObjectId;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevertResult {
    private boolean success;

    private UUID repository;

    private String branch;

    private ObjectId revertCommit;

    private ObjectId revertedCommit;
}
