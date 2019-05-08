package org.geogig.server.service.rpc;

import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MergeResult {
    private boolean success;

    private RevCommit mergeCommit;

    private RevCommit theirsCommit;

    private RevCommit oursCommit;

    private RevCommit commonAncestor;

    private MergeScenarioReport report;
}
