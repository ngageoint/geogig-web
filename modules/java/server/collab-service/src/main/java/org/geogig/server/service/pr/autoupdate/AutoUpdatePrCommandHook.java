package org.geogig.server.service.pr.autoupdate;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.Nullable;
import org.geogig.server.geogig.ServerContext;
import org.geogig.server.model.PullRequest;
import org.geogig.server.service.pr.PullRequestService;
import org.locationtech.geogig.hooks.CommandHook;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.UpdateRef;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.impl.GeogigTransaction;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutoUpdatePrCommandHook implements CommandHook {

    public static final String DISABLE = AutoUpdatePrCommandHook.class.getName() + ".DISABLE";

    private java.util.Optional<PullRequestService> workserService(AbstractGeoGigOp<?> command) {
        PullRequestService service = null;
        Context context = command.context();
        if (!isInTransaction(command) && context instanceof ServerContext) {
            service = ((ServerContext) context).bean(PullRequestService.class);
        }
        return java.util.Optional.ofNullable(service);
    }

    public @Override boolean appliesTo(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return UpdateRef.class.equals(clazz);
    }

    public @Override <C extends AbstractGeoGigOp<?>> C pre(C command) {
        UpdateRef updateRef = (UpdateRef) command;
        String name = updateRef.getName();

        if (!Ref.isChild(Ref.HEADS_PREFIX, name)) {
            return command;
        }

        workserService(command).ifPresent(service -> {
            Optional<Ref> branch = command.context().command(RefParse.class).setName(name).call();
            Optional<URI> repositoryURI = command.context().command(ResolveGeogigURI.class).call();
            if (repositoryURI.isPresent() && branch.isPresent()) {
                CompletableFuture<List<PullRequest>> requestsAffected;
                requestsAffected = service.findPullRequestsAffectedByBranch(repositoryURI.get(),
                        branch.get());
                updateRef.getClientData().put(AutoUpdatePrCommandHook.class, requestsAffected);
            }
        });
        return command;
    }

    @SuppressWarnings("unchecked")
    public @Override <T> T post(AbstractGeoGigOp<T> command, @Nullable Object retVal,
            @Nullable RuntimeException exception) {

        final com.google.common.base.Optional<Ref> result = (Optional<Ref>) retVal;

        CompletableFuture<List<PullRequest>> requestsAffected;

        requestsAffected = (CompletableFuture<List<PullRequest>>) command.getClientData()
                .get(AutoUpdatePrCommandHook.class);

        if (null != requestsAffected && result.isPresent()) {
            workserService(command).ifPresent(service -> {
                List<PullRequest> prs;
                try {
                    prs = requestsAffected.get();
                    prs.forEach(pr -> {

                        PullRequest prcurrent = service.get(pr.getRepositoryId(), pr.getId())
                                .orElse(null);
                        if (prcurrent != null && !prcurrent.isOpen()) {
                            return;
                        }
                        log.info(
                                "Automatically updating test merge for pull request #{} '{}' due to update of branch {}",
                                pr.getId(), pr.getTitle(), result.get());
                        service.checkMergeableAbortIfRunning(pr);
                    });
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("Error getting pull requests affected by UpdateRef", e);
                }
            });
        }

        return (T) retVal;
    }

    private boolean isInTransaction(AbstractGeoGigOp<?> command) {
        return command.context() instanceof GeogigTransaction;
    }
}
