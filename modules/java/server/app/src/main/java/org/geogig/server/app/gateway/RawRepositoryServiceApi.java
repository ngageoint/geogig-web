package org.geogig.server.app.gateway;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.branch.ConflictTuple;
import org.geogig.server.service.branch.DiffSummary;
import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.MergeRequest;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.RevisionObject;
import org.geogig.web.server.api.RawRepositoryAccessApiDelegate;
import org.geogig.web.server.api.RepositoryManagementApi;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated
 * {@link RepositoryManagementApi}, handles the REST request/response/error handling aspects of the
 * API, and delegates business logic to a {@link RepositoryManagementService}.
 */
public @Service class RawRepositoryServiceApi extends AbstractService
        implements RawRepositoryAccessApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired RepositoryManagementService repositories;

    private @Autowired RepositoryRPCService repos;

    private @Autowired BranchAdminService branches;

    private @Autowired UserService users;

    private <T> ResponseEntity<T> run(String user, String repoName,
            Function<Repository, ResponseEntity<T>> task) {

        Optional<RepoInfo> repo = repositories.getByName(user, repoName);

        if (repo.isPresent()) {
            // return task.apply(repo.get());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    public @Override ResponseEntity<RevisionObject> getObject(String user, String repoName,
            String objectId) {

        return run(user, repoName, (repo) -> {
            ObjectId id = GeogigObjectModelBridge.toId(objectId);
            RevObject o = repo.objectDatabase().getIfPresent(id);
            if (null == o) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            RevisionObject revisionObject = GeogigObjectModelBridge.map(o);
            return new ResponseEntity<>(revisionObject, HttpStatus.OK);
        });
    }

    //@formatter:off
    public @Override ResponseEntity<List<RevisionCommit>> log(
            @NonNull String user,
            @NonNull String repo,
            @Nullable UUID txId,
            @Nullable String head,
            @Nullable List<String> path,
            @Nullable Integer limit) {
        //@formatter:on
        RepoInfo repositoryInfo = repositories.getOrFail(user, repo);
        UUID repoId = repositoryInfo.getId();
        return super.ok(() -> Lists
                .newArrayList(Iterators.transform(repos.getCommits(repoId, head, path, limit, txId),
                        GeogigObjectModelBridge::toCommit)));
    }

    //@formatter:off
    public @Override ResponseEntity<List<LayerDiffSummary>> diffSummary(
            @NonNull String user,
            @NonNull String repo,
            @NonNull String left,
            @NonNull String right,
             String rightUser,
             String rightRepo,
            UUID txId) {
        //@formatter:on

        return super.ok(() -> {
            RepoInfo repository = repositories.getOrFail(user, repo);
            RepoInfo rightRepository = repository;
            if ((rightRepo != null) && (rightUser != null))
                rightRepository = repositories.getOrFail(rightUser, rightRepo);

            DiffSummary diffSummary = repos.diffSummary(repository.getId(), rightRepository.getId(),
                    left, right, txId, null);
            return presentation.toInfo(diffSummary);
        });
    }

    public @Override ResponseEntity<AsyncTaskInfo> merge(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @Nullable UUID txId,
            @NonNull MergeRequest mergeRequest) {//@formatter:on

        org.geogig.server.service.rpc.MergeRequest modelReq = presentation.toModel(mergeRequest);
        return super.ok(() -> presentation.toInfo(repos.merge(user, repo, txId, modelReq)));
    }

    public @Override ResponseEntity<Long> countConflicts(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            @Nullable String path) {//@formatter:on

        return super.ok(() -> repos.countConflicts(user, repo, txId, path));
    }

    public @Override ResponseEntity<List<ConflictInfo>> getConflicts(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            @Nullable String path,
            @Nullable Boolean details,
            @Nullable Integer page,
            @Nullable Integer pageSize) {//@formatter:on

        boolean detailed = details == null ? false : details.booleanValue();
        int p = page == null ? 1 : page;
        int ps = pageSize == null ? 1000 : pageSize;

        Iterator<ConflictTuple> conflicts = repos.getConflicts(user, repo, txId, path, detailed, p,
                ps);
        List<ConflictInfo> res = Lists.transform(Lists.newArrayList(conflicts),
                presentation::toInfo);

        return super.ok(res);
    }

    public @Override ResponseEntity<AsyncTaskInfo> stage(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull UUID txId,
            List<String> paths) {//@formatter:on

        return super.ok(() -> presentation.toInfo(repos.stage(user, repo, txId, paths)));
    }

}
