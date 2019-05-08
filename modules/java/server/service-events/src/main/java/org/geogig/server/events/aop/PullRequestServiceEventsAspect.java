package org.geogig.server.events.aop;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geogig.server.events.model.PullRequestEvent;
import org.geogig.server.events.model.PullRequestEvent.Created;
import org.geogig.server.events.model.PullRequestStatusEvent;
import org.geogig.server.model.AuthUser;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.User;
import org.geogig.server.service.async.Task;
import org.springframework.context.annotation.Configuration;

import lombok.NonNull;

@Aspect
@Configuration
public class PullRequestServiceEventsAspect extends AbstractServiceEventAspect {

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.prCreateExecution()", returning = "pr")
    public void afterPrCreated(JoinPoint joinPoint, PullRequest pr) {
        String createdBy = pr.getCreatedBy();
        Optional<AuthUser> caller = auth.getUserByName(createdBy);
        Created event = PullRequestEvent.Created.builder().request(pr).caller(caller).build();
        publishEvent(event);
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.prCloseExecution()", returning = "pr")
    public void afterPrClosed(JoinPoint joinPoint, PullRequest pr) {
        UUID closedByUser = pr.getClosedByUserId();
        Optional<AuthUser> caller = auth.getUser(closedByUser);
        publishEvent(PullRequestEvent.Updated.builder().request(pr).caller(caller).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.prModifyExecution()", returning = "pr")
    public void updatePullRequest(JoinPoint joinPoint, PullRequest pr) {
        String modifiedBy = pr.getModifiedBy();
        Optional<AuthUser> caller = auth.getUserByName(modifiedBy);
        publishEvent(PullRequestEvent.Updated.builder().request(pr).caller(caller).build());
    }

    @Around("org.geogig.server.events.aop.PointCuts.prMergeExecution()")
    public Task<PullRequestStatus> onPrMerge(ProceedingJoinPoint joinPoint) throws Throwable {

        User callerUser = (User) joinPoint.getArgs()[0];
        final Optional<AuthUser> caller = auth.getUser(callerUser.getId());

        @SuppressWarnings("unchecked")
        Task<PullRequestStatus> task = (Task<PullRequestStatus>) joinPoint.proceed();

        CompletableFuture<PullRequestStatus> future = task.getFuture();
        future.whenCompleteAsync((status, error) -> {
            if (status != null) {
                publishStatusEvent(caller, status);
                publishMergeResult(caller, status);
            } else {
                error.printStackTrace();
            }
        });
        return task;
    }

    private void publishMergeResult(Optional<AuthUser> caller, @NonNull PullRequestStatus status) {
        if (status.isClosed() || status.isMerged()) {
            PullRequest pr = status.getRequest();
            publishEvent(PullRequestEvent.Updated.builder().request(pr).caller(caller).build());
        }
    }

    @Around("org.geogig.server.events.aop.PointCuts.prUpdateStatusExecution()")
    public CompletableFuture<PullRequestStatus> onCheckPrStatus(ProceedingJoinPoint joinPoint)
            throws Throwable {

        final Optional<AuthUser> caller = auth.getCurrentUser();

        @SuppressWarnings("unchecked")
        CompletableFuture<PullRequestStatus> future = (CompletableFuture<PullRequestStatus>) joinPoint
                .proceed();

        future.whenCompleteAsync((status, exception) -> {
            if (status != null) {
                publishStatusEvent(caller, status);
            }
        });
        return future;
    }

    private void publishStatusEvent(final Optional<AuthUser> caller, PullRequestStatus status) {
        PullRequestStatusEvent event = PullRequestStatusEvent.builder().status(status)
                .caller(caller).build();
        publishEvent(event);
    }
}
