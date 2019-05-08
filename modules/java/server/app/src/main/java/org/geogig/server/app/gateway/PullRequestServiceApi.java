package org.geogig.server.app.gateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus.MergeableStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.StoredDiffSummary;
import org.geogig.server.model.StoredDiffSummary.Bounds;
import org.geogig.server.model.User;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.branch.DiffSummary;
import org.geogig.server.service.feature.FeatureService;
import org.geogig.server.service.pr.PullRequestService;
import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.FeatureCollection;
import org.geogig.web.model.FeatureQuery;
import org.geogig.web.model.GeogigFeatureCollection;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestRequest;
import org.geogig.web.model.PullRequestStatus;
import org.geogig.web.model.RequestRequestPatch;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.geotools.GeoToolsDomainBridge;
import org.geogig.web.server.api.FeatureServiceApi;
import org.geogig.web.server.api.PullRequestsApiDelegate;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link FeatureServiceApi},
 * handles the REST request/response/error handling aspects of the API, and delegates business logic
 * to a {@link BranchAdminService} or {@link PullRequestService}.
 */
public @Service class PullRequestServiceApi extends AbstractService
        implements PullRequestsApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired RepositoryManagementService repos;

    private @Autowired TransactionService tx;

    private @Autowired FeatureService features;

    private @Autowired UserService users;

    private @Autowired PullRequestService pullRequests;

    public @Override ResponseEntity<PullRequestInfo> createPullRequest(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull PullRequestRequest request) {//@formatter:on

        RepoInfo targetRepo = repos.getOrFail(user, repo);
        return super.create(() -> presentation
                .toInfo(pullRequests.create(presentation.toModel(targetRepo, request))));
    }

    public @Override ResponseEntity<PullRequestInfo> getPullRequest(//@formatter:off
            @NonNull String user, 
            @NonNull String repo, 
            @NonNull Integer prId) {//@formatter:on

        return super.okOrNotFound(
                pullRequests.getPullRequest(user, repo, prId).map(presentation::toInfo));
    }

    public @Override ResponseEntity<PullRequestStatus> getPullRequestStatus(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer pr) {//@formatter:on

        return super.ok(() -> {
            PullRequest request = pullRequests.getOrFail(user, repo, pr);
            org.geogig.server.model.PullRequestStatus status;
            try {
                status = pullRequests.getStatus(request).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(Throwables.getRootCause(e));
            }
            return presentation.toInfo(status);
        });
    }

    public @Override ResponseEntity<List<PullRequestInfo>> listPullRequests(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Boolean open,
            @NonNull Boolean closed) {//@formatter:on

        return super.ok(() -> Lists.newArrayList(Iterables.transform(
                pullRequests.listPullRequests(user, repo, open, closed), presentation::toInfo)));
    }

    public @Override ResponseEntity<List<RevisionCommit>> getPullRequestCommits(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer pr) {//@formatter:on

        return super.ok(() -> Lists.newArrayList(Iterators.transform(
                pullRequests.getCommits(user, repo, pr), GeogigObjectModelBridge::toCommit)));
    }

    /**
     * @ApiResponse(code = 200, message = "Returns the diff summary between the current state of the
     *                   issuer and target branches", response = LayerDiffSummary.class,
     *                   responseContainer = "List"),
     * @ApiResponse(code = 409, message = "Temporary conflict with the internal state of the
     *                   resource; try later. The test-merge commit is being computed and cannot yet
     *                   provide a diff."),
     * @ApiResponse(code = 428, message = "Precondition Required, the PR is in a conflicting state
     *                   and cannot provide a diff because there is no test-merge commit") })
     * 
     */
    public @Override ResponseEntity<List<LayerDiffSummary>> getPullRequestDiffSummary(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer pr) {//@formatter:on

        final PullRequest preq = pullRequests.getOrFail(user, repo, pr);
        final org.geogig.server.model.PullRequestStatus status;
        status = pullRequests.getStatus(preq).join();

        final MergeableStatus mergeable = status.getMergeable();
        switch (mergeable) {
        case CHECKING:
        case UNKNOWN:
        case MERGING:
            return super.error(HttpStatus.CONFLICT,
                    "Status of pull request %s:%s/#%s is being computed, try again later", user,
                    repo, pr);
        case UNMERGEABLE:
            return super.error(HttpStatus.PRECONDITION_REQUIRED,
                    "Pull request %s:%s/#%s has merge conflicts, can't compute diff summary", user,
                    repo, pr);
        case MERGEABLE:
        case MERGED:
            // ok to proceed
            break;
        }

        return super.ok(() -> {
            DiffSummary diffSummary = pullRequests.getDiffSummary(user, repo, pr);
            List<LayerDiffSummary> layerSummaries = presentation.toInfo(diffSummary);
            return layerSummaries;
        });
    }

    public @Override ResponseEntity<AsyncTaskInfo/* <MergeResult> */> mergePullRequest(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer pr,
            @Nullable String commitTitle,
            @Nullable String commitMessage) {//@formatter:on

        return super.ok(() -> presentation
                .toInfo(pullRequests.merge(user, repo, pr, commitTitle, commitMessage)));
    }

    public @Override ResponseEntity<PullRequestInfo> closePullRequest(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer pr) {//@formatter:on

        final User caller = users.requireAuthenticatedUser();

        return super.ok(
                () -> presentation.toInfo(pullRequests.close(caller.getId(), user, repo, pr)));
    }

    public @Override ResponseEntity<PullRequestInfo> updatePullRequest(String user, String repo,
            Integer pr, RequestRequestPatch requestRequestPatch) {

        String title = requestRequestPatch.getTitle();
        String description = requestRequestPatch.getDescription();
        Boolean open = requestRequestPatch.isOpen();
        String targetBranch = requestRequestPatch.getTargetBranch();
        return super.ok(() -> presentation.toInfo(pullRequests.updatePullRequest(user, repo, pr,
                title, description, open, targetBranch)));
    }

    /**
     * @ApiResponse(code = 200, message = "successful operation, Diff FeatureCollection for the
     *                   requested PR layer", response = FeatureCollection.class),
     * @ApiResponse(code = 404, message = "PR or Layer not found"),
     * @ApiResponse(code = 409, message = "Temporary conflict with the internal state of the
     *                   resource; try later. The test-merge commit is being computed and cannot yet
     *                   provide a diff."),
     * @ApiResponse(code = 428, message = "Precondition Required, the PR is in a conflicting state
     *                   and cannot provide a diff because there is no test-merge commit") })
     * 
     */
    public @Override ResponseEntity<FeatureCollection> getPullRequestDiffFeatures(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer prId,
            @NonNull String layerName) {//@formatter:on

        final PullRequest pr = pullRequests.getOrFail(user, repo, prId);
        final org.geogig.server.model.PullRequestStatus status;
        status = pullRequests.getStatus(pr).join();

        final MergeableStatus mergeable = status.getMergeable();
        switch (mergeable) {
        case CHECKING:
        case UNKNOWN:
        case MERGING:
            return super.error(HttpStatus.CONFLICT,
                    "Status of pull request %s:%s/#%s is being computed, try again later", user,
                    repo, prId);
        case UNMERGEABLE:
            return super.error(HttpStatus.PRECONDITION_REQUIRED,
                    "Pull request %s:%s/#%s has merge conflicts, can't compute diff features", user,
                    repo, prId);
        case MERGEABLE:
        case MERGED:
            // ok
            break;
        }
        return super.ok(() -> getPrLayerFeatures(status, layerName));
    }

    /**
     * @ApiResponse(code = 200, message = "successful operation, Conflicts FeatureCollection for the
     *                   requested PR layer", response = FeatureCollection.class),
     * @ApiResponse(code = 404, message = "PR or Layer not found"),
     * @ApiResponse(code = 409, message = "Temporary conflict with the internal state of the
     *                   resource; try later. The test-merge commit is being computed and cannot yet
     *                   provide a diff."),
     * @ApiResponse(code = 428, message = "Precondition Required, the PR test merge succeeded and
     *                   hence there are no conflicts to report") })
     * 
     */
    public @Override ResponseEntity<FeatureCollection> getPullRequestConflictFeatures(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull Integer prId,
            @NonNull String layerName) {//@formatter:on

        final PullRequest pr = pullRequests.getOrFail(user, repo, prId);
        final org.geogig.server.model.PullRequestStatus status;
        status = pullRequests.getStatus(pr).join();

        final MergeableStatus mergeable = status.getMergeable();
        switch (mergeable) {
        case CHECKING:
        case UNKNOWN:
        case MERGING:
            return super.error(HttpStatus.CONFLICT,
                    "Status of pull request %s:%s/#%s is being computed, try again later", user,
                    repo, prId);
        case MERGEABLE:
        case MERGED:
            return super.error(HttpStatus.PRECONDITION_REQUIRED,
                    "Pull request %s:%s/#%s has no merge conflicts", user, repo, prId);
        case UNMERGEABLE:
            break;
        }

        final UUID transactionId = status.getTransaction();

        return super.ok(() -> {

            FeatureQuery query = new FeatureQuery();
            Context context;
            try {
                context = tx.resolveContext(pr.getRepositoryId(), transactionId);
            } catch (RepositoryConnectionException e) {
                throw new IllegalStateException(e);
            }

            GeogigFeatureCollection conflictingFeatures;
            conflictingFeatures = features.getConflictingFeatures(context, layerName, query);
            return conflictingFeatures;
        });
    }

    private GeogigFeatureCollection getPrLayerFeatures(
            @NonNull org.geogig.server.model.PullRequestStatus status, @NonNull String layerName) {

        final PullRequest pullRequest = status.getRequest();
        final @Nullable UUID transactionId = status.getTransaction();
        final String mergeCommitRef = status.getMergeRef();
        final String headRef = status.getHeadRef();
        final UUID targetRepoId = pullRequest.getRepositoryId();

        CompletableFuture<DiffSummary> diffSummary;
        diffSummary = CompletableFuture.supplyAsync(() -> pullRequests.getDiffSummary(pullRequest));

        FeatureQuery query = new FeatureQuery();
        query.setHead(mergeCommitRef);
        query.setOldHead(headRef);

        GeogigFeatureCollection diffFeatureCollection;
        diffFeatureCollection = features.getFeatures(targetRepoId, transactionId, layerName, query);

        try {
            DiffSummary summary = diffSummary.get();
            Optional<StoredDiffSummary> layerSummary = summary.getLayerDiffSummary().stream()
                    .filter(d -> d.getPath().equals(layerName)).findFirst();
            layerSummary.ifPresent(s -> {
                long size = s.getFeaturesAdded() + s.getFeaturesChanged() + s.getFeaturesRemoved();
                Bounds lb = s.getLeftBounds();
                Bounds rb = s.getRightBounds();
                BoundingBox bounds = join(lb, rb);
                diffFeatureCollection.setSize(size);
                diffFeatureCollection.setBounds(bounds);
            });
        } catch (InterruptedException | ExecutionException e) {
            // ignore
        }

        return diffFeatureCollection;
    }

    private BoundingBox join(Bounds lb, Bounds rb) {
        Envelope env = new Envelope();
        if (lb != null) {
            env.expandToInclude(lb.getMinX(), lb.getMinY());
            env.expandToInclude(lb.getMaxX(), lb.getMaxY());
        }
        if (rb != null) {
            env.expandToInclude(rb.getMinX(), rb.getMinY());
            env.expandToInclude(rb.getMaxX(), rb.getMaxY());
        }
        return GeoToolsDomainBridge.toBounds(env);
    }

}
