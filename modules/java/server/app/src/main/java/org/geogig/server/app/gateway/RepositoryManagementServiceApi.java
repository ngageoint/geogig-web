package org.geogig.server.app.gateway;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.User;
import org.geogig.server.service.async.AsyncTaskProgress;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.async.Job;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.user.UserService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.server.api.RepositoryManagementApi;
import org.geogig.web.server.api.RepositoryManagementApiDelegate;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.ProgressListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * API delegate implementation for the swagger-codegen auto-generated
 * {@link RepositoryManagementApi}, handles the REST request/response/error handling aspects of the
 * API, and delegates business logic to a {@link RepositoryManagementService}.
 */
public @Service class RepositoryManagementServiceApi extends AbstractService
        implements RepositoryManagementApiDelegate {

    private @Autowired PresentationService presentation;

    private @Autowired RepositoryManagementService repositories;

    private @Autowired UserService users;

    private @Autowired AsyncTasksService async;

    public @Override Optional<ObjectMapper> getObjectMapper() {
        return Optional.ofNullable(objectMapper);
    }

    public @Override Optional<HttpServletRequest> getRequest() {
        return Optional.ofNullable(currentRequest);
    }

    public @Override ResponseEntity<List<RepositoryInfo>> listRepositories(List<String> topics) {
        return super.ok(Lists
                .newArrayList(Iterables.transform(repositories.getAll(), presentation::toInfo)));
    }

    public @Override ResponseEntity<List<RepositoryInfo>> listUserRepositories(String user) {
        return super.ok(Lists.transform(repositories.getByUser(user), presentation::toInfo));
    }

    public @Override ResponseEntity<RepositoryInfo> getRepository(String user, String repoName) {
        return super.okOrNotFound(repositories.getByName(user, repoName).map(presentation::toInfo));
    }

    public @Override ResponseEntity<RepositoryInfo> createRepository(String user, String repo,
            @Nullable String targetStore, @Nullable IdentifiedObject metadata) {

        String description = metadata == null ? null : metadata.getDescription();
        return super.create(() -> presentation
                .toInfo(repositories.create(user, repo, targetStore, description)));
    }

    public @Override ResponseEntity<Void> deleteRepository(String user, String repo) {

        return super.run(HttpStatus.NO_CONTENT, ((Runnable) () -> repositories.remove(user, repo)));
    }

    public @Override ResponseEntity<RepositoryInfo> modifyRepository(@NonNull String user,
            @NonNull String repo, @NonNull RepositoryInfo repository) {

        return super.ok(() -> {
            User caller = users.requireAuthenticatedUser();
            RepoInfo currentObject = repositories.getOrFail(user, repo);
            IdentifiedObject providedOwnerInfo = repository.getOwner();
            Preconditions.checkArgument(null != providedOwnerInfo,
                    "No repository owner provided as part of the body");
            Preconditions.checkArgument(
                    caller.isSiteAdmin() || caller.getId().equals(providedOwnerInfo.getId()),
                    "User has no rights to modify this repository");
            RepoInfo repoInfo = presentation.toModel(repository);
            RepoInfo updated = repositories.update(repoInfo);
            return presentation.toInfo(updated);
        });
    }

    public @Override ResponseEntity<RepositoryInfo> modifyRepositoryById(
            @NonNull RepositoryInfo repository) {

        return super.ok(() -> {
            RepoInfo repo = presentation.toModel(repository);
            RepoInfo current = repositories.get(repo.getId())
                    .or(() -> repositories.getOrphanByStore(repo.getStoreId(), repo.getId()))//
                    .orElseThrow(() -> new NoSuchElementException());
            boolean isOrphan = null == current.getOwnerId();
            RepoInfo updated = isOrphan ? repositories.updateOrphan(repo)
                    : repositories.update(repo);
            return presentation.toInfo(updated);
        });
    }

    //@formatter:off
    public @Override ResponseEntity<List<RepositoryInfo>> getConstellation(
            String user,
            String repo) {
        //@formatter:on

        return super.ok(() -> repositories.getConstellationOf(user, repo).stream()
                .map(presentation::toInfo).collect(Collectors.toList()));
    }

    //@formatter:off
    public @Override ResponseEntity<List<RepositoryInfo>> listForks(
            @NonNull String user, 
            @NonNull String repo, 
            Boolean recursive) {
        //@formatter:on

        Set<RepoInfo> forks = repositories.getForksOf(user, repo,
                recursive == null ? false : recursive.booleanValue());
        List<RepositoryInfo> infos = forks.stream().map(presentation::toInfo)
                .collect(Collectors.toList());
        return super.ok(infos);
    }

    //@formatter:off
    public @Override ResponseEntity<AsyncTaskInfo> forkRepository(
            String user, 
            String repo,
            @Nullable String forkName, 
            @Nullable String targetStore) {
      //@formatter:on

        User targetOwner = users.requireAuthenticatedUser();
        RepoInfo origin = repositories.getOrFail(user, repo);
        User originOwner = users.getOrFail(origin.getOwnerId());
        ForkJob job = ForkJob.builder().origin(origin).originOwner(originOwner).caller(targetOwner)
                .targetOwner(targetOwner).forkName(forkName).targetStore(targetStore).build();

        return super.ok(() -> {
            Task<RepoInfo> taskInfo = async.submit(job);
            return presentation.toInfo(taskInfo);
        });
    }

    @Builder
    @Data
    private static class ForkJob implements Job<RepoInfo> {

        private @NonNull RepoInfo origin;

        private @NonNull User caller, originOwner, targetOwner;

        private String forkName, targetStore;

        private final ProgressListener progress = new DefaultProgressListener();

        public @Override CompletableFuture<RepoInfo> run(ApplicationContext context) {
            RepositoryManagementService repositories = context
                    .getBean(RepositoryManagementService.class);

            String targetName = forkName == null ? origin.getIdentity() : forkName;
            String targetStore = getTargetStore();

            CompletableFuture<RepoInfo> future;
            future = repositories.fork(caller, origin, targetOwner, targetName, targetStore,
                    progress);
            return future;
        }

        public @Override String getDescription() {
            return String.format("Fork %s:%s to %s:%s", originOwner.getIdentity(),
                    origin.getIdentity(), targetOwner.getIdentity(),
                    forkName == null ? origin.getIdentity() : forkName);
        }

        public @Override Optional<UUID> getTransaction() {
            return Optional.empty();
        }

        public @Override UUID getCallerUser() {
            return caller.getId();
        }

        public @Override AsyncTaskProgress getProgressListener() {
            AsyncTaskProgress info = new AsyncTaskProgress();
            ProgressListener pl = this.progress;
            if (pl != null) {
                info.setTaskDescription(pl.getDescription());
                info.setProgressDescription(pl.getProgressDescription());
                info.setMaxProgress((double) pl.getMaxProgress());
                info.setProgress((double) pl.getProgress());
            }
            return info;
        }
    }
}