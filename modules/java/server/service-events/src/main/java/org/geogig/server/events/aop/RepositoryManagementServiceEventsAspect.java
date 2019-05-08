package org.geogig.server.events.aop;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geogig.server.events.model.RepositoryEvent;
import org.geogig.server.events.model.RepositoryEvent.Forked;
import org.geogig.server.model.AuthUser;
import org.geogig.server.model.RepoInfo;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class RepositoryManagementServiceEventsAspect extends AbstractServiceEventAspect {

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.repositoryCreateExecution()", returning = "repo")
    public void afterRepositoryCreated(JoinPoint joinPoint, RepoInfo repo) {
        publishEvent(RepositoryEvent.Created.builder().repository(repo).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.repositoryModifyExecution()", returning = "repo")
    public void afterRepositoryModified(JoinPoint joinPoint, RepoInfo repo) {
        publishEvent(RepositoryEvent.Updated.builder().repository(repo).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.repositoryDeleteExecution()", returning = "repo")
    public void afterRepositoryDeleted(JoinPoint joinPoint, RepoInfo repo) {
        publishEvent(RepositoryEvent.Deleted.builder().repository(repo).build());
    }

    @SuppressWarnings("unchecked")
    @Around("org.geogig.server.events.aop.PointCuts.repositoryForkExecution()")
    public CompletableFuture<RepoInfo> aroundRepositoryFork(ProceedingJoinPoint joinPoint)
            throws Throwable {

        CompletableFuture<RepoInfo> future = (CompletableFuture<RepoInfo>) joinPoint.proceed();

        future.whenComplete((fork, exception) -> {
            if (exception == null) {
                String createdBy = fork.getCreatedBy();
                Optional<AuthUser> caller = auth.getUserByName(createdBy);
                Forked event = RepositoryEvent.Forked.builder().caller(caller).repository(fork)
                        .build();
                publishEvent(event);
            }
        });
        return future;
    }

}
