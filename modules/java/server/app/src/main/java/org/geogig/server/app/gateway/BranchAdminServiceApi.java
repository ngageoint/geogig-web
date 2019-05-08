package org.geogig.server.app.gateway;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.User;
import org.geogig.server.service.branch.Branch;
import org.geogig.server.service.branch.BranchAdminService;
import org.geogig.server.service.pr.PullRequestService;
import org.geogig.server.service.presentation.GeogigObjectModelBridge;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.PullArgs.PullArgsBuilder;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.BranchInfo;
import org.geogig.web.model.CommitDiffSummary;
import org.geogig.web.model.PullArgs;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.server.api.BranchesApiDelegate;
import org.geogig.web.server.api.FeatureServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link FeatureServiceApi},
 * handles the REST request/response/error handling aspects of the API, and delegates business logic
 * to a {@link BranchAdminService} or {@link PullRequestService}.
 */
public @Service class BranchAdminServiceApi extends AbstractService implements BranchesApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired RepositoryManagementService repos;

    private @Autowired UserService users;

    private @Autowired BranchAdminService branches;

    public @Override ResponseEntity<BranchInfo> createBranch(//@formatter:off
            String user, 
            String repo,
            String branch, 
            String commitish, 
            @Nullable UUID txId, 
            @Nullable String description) {//@formatter:on

        return super.create(() -> presentation.toBranchInfo(
                branches.createBranch(user, repo, branch, commitish, description, txId)));
    }

    public @Override ResponseEntity<BranchInfo> checkout(//@formatter:off
            String user,
            String repo,
            String branch,
            UUID geogigTransactionId,
            Boolean force) {//@formatter:on

        boolean forceCheckout = force == null ? false : force.booleanValue();
        return super.ok(() -> presentation.toBranchInfo(
                branches.checkout(user, repo, branch, geogigTransactionId, forceCheckout)));
    }

    public @Override ResponseEntity<Void> deleteBranch(//@formatter:off
            String user, 
            String repo, 
            String branch,
            @Nullable UUID txId) {//@formatter:on

        return super.run(HttpStatus.NO_CONTENT,
                () -> branches.deleteBranch(user, repo, branch, txId));
    }

    public @Override ResponseEntity<BranchInfo> getBranch(//@formatter:off
            String user, 
            String repo, 
            String branch,
            @Nullable UUID txId) {//@formatter:on

        return super.ok(
                () -> presentation.toBranchInfo(branches.getBranch(user, repo, branch, txId)));
    }

    public @Override ResponseEntity<BranchInfo> getCurrentBranch(//@formatter:off
            String user, 
            String repo,
            @Nullable UUID txId) {//@formatter:on

        return super.ok(
                () -> presentation.toBranchInfo(branches.getCurrentBranch(user, repo, txId)));
    }

    public @Override ResponseEntity<List<BranchInfo>> listBranches(//@formatter:off
            @NonNull String user, 
            @NonNull String repo,
            @Nullable UUID txId) {//@formatter:on

        return super.ok(() -> Lists.transform(branches.listBranches(user, repo, txId),
                presentation::toBranchInfo));
    }

    public @Override ResponseEntity<List<RevisionCommit>> getBranchCommits(//@formatter:off
            @NonNull String user, 
            @NonNull String repo,
            @NonNull String branch, 
            @Nullable UUID txId) {//@formatter:on

        return super.ok(
                () -> Lists.newArrayList(Iterators.transform(branches.log(user, repo, branch, txId),
                        GeogigObjectModelBridge::toCommit)));
    }

    public @Override ResponseEntity<Boolean> compareBranches(//@formatter:off
            @NonNull String user1,
            @NonNull String repo1,
            @NonNull String branch1,
            @NonNull String user2,
            @NonNull String repo2,
            @NonNull String branch2) {//@formatter:on
        RepoInfo left = repos.getOrFail(user1, repo1);
        RepoInfo right = repos.getOrFail(user2, repo2);
        return super.ok(() -> branches.compare(left.getId(), branch1, right.getId(), branch2));
    }

    public @Override ResponseEntity<Boolean> conflictsWith(//@formatter:off
            @NonNull String user1,
            @NonNull String repo1,
            @NonNull String branch1,
            @NonNull String user2,
            @NonNull String repo2,
            @NonNull String branch2) {//@formatter:on
        RepoInfo left = repos.getOrFail(user1, repo1);
        RepoInfo right = repos.getOrFail(user2, repo2);
        return super.ok(() -> branches.conflict(left.getId(), branch1, right.getId(), branch2));
    }

    public @Override ResponseEntity<RevisionCommit> findCommonAncestor(//@formatter:off
            @NonNull String user1,
            @NonNull String repo1,
            @NonNull String branch1,
            @NonNull String user2,
            @NonNull String repo2,
            @NonNull String branch2) {//@formatter:on
        UUID left = repos.getOrFail(user1, repo1).getId();
        UUID right = repos.getOrFail(user2, repo2).getId();
        return super.okOrNotFound(branches.commonAncestor(left, branch1, right, branch2)
                .map(GeogigObjectModelBridge::toCommit));
    }

    public @Override ResponseEntity<CommitDiffSummary> findCommitDifferences(//@formatter:off
            String user,
            String repo,
            String branch,
            String user2,
            String repo2,
            String branch2) {//@formatter:on

        UUID left = repos.getOrFail(user, repo).getId();
        UUID right = repos.getOrFail(user2, repo2).getId();
        return super.ok(() -> GeogigObjectModelBridge
                .toCommitDiffSummary(branches.commitDifferences(left, branch, right, branch2)));

    }

    public @Override ResponseEntity<RevisionCommit> resetHard(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull String branch,
            @NonNull String commitIsh,
            @Nullable UUID txId) {//@formatter:on

        return super.ok(() -> GeogigObjectModelBridge
                .toCommit(branches.resetHard(user, repo, branch, commitIsh, txId)));
    }

    public @Override ResponseEntity<AsyncTaskInfo/* <RevertResults> */> revertCommit(//@formatter:off
            @NonNull String user,
            @NonNull String repo,
            @NonNull String branch,
            @NonNull String commitIsh,
            @Nullable UUID txId) {//@formatter:on

        return super.ok(() -> presentation
                .toInfo(branches.revertCommit(user, repo, branch, commitIsh, txId)));
    }

    public @Override ResponseEntity<AsyncTaskInfo> syncBranch(//@formatter:off
            @NonNull String user, 
            @NonNull String repo,
            @NonNull String branch, 
            @NonNull UUID txId, 
            @Nullable PullArgs args) {//@formatter:on
        if (args == null) {
            args = new PullArgs();
        }
        final User caller = users.requireAuthenticatedUser();
        final Branch targetBranch = branches.getBranch(user, repo, branch, txId);
        final RepoInfo targetRepo = repos.getOrFail(user, repo);

        String commitMessage = args.getCommitMessage();
        String remoteRepoHead = args.getRemoteRepoHead();
        String remoteRepoName = args.getRemoteRepoName();
        String remoteRepoOwner = args.getRemoteRepoOwner();
        Boolean ours = args.isMergeStrategyOurs();
        Boolean theirs = args.isMergeStrategyTheirs();
        Boolean noFf = args.isNoFf();

        RepoInfo remoteRepo;
        if (remoteRepoOwner != null && remoteRepoName != null) {
            remoteRepo = repos.getOrFail(remoteRepoOwner, remoteRepoName);
        } else {
            remoteRepo = targetBranch.getRemote().orElseThrow(() -> new IllegalArgumentException(
                    String.format("Branch %s is not tracking a remote branch", branch)));
        }
        if (remoteRepoHead == null) {
            remoteRepoHead = targetBranch.getRemoteBranch().orElse(branch);
        }

        PullArgsBuilder builder = org.geogig.server.service.rpc.PullArgs.builder();
        builder.targetRepo(targetRepo.getId())//
                .targetBranch(targetBranch.getName())//
                .remoteRepo(remoteRepo.getId())//
                .remoteBranch(remoteRepoHead)//
                .commitMessage(commitMessage)//
                .mergeStrategyOurs(ours == null ? false : ours.booleanValue())//
                .mergeStrategyTheirs(theirs == null ? false : theirs.booleanValue())//
                .noFf(noFf == null ? false : noFf.booleanValue())//
                .build();

        org.geogig.server.service.rpc.PullArgs serviceParams = builder.build();

        return super.ok(() -> presentation
                .toInfo(branches.syncWithRemoteBranch(caller, txId, branch, serviceParams)));
    }
}
