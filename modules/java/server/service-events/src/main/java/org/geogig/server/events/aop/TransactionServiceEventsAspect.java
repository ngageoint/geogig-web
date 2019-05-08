package org.geogig.server.events.aop;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.geogig.server.events.model.TransactionEvent;
import org.geogig.server.events.model.TransactionEvent.Aborted;
import org.geogig.server.events.model.TransactionEvent.Committed;
import org.geogig.server.events.model.TransactionEvent.Committed.CommittedBuilder;
import org.geogig.server.events.model.TransactionEvent.Created;
import org.geogig.server.events.model.TransactionEvent.Deleted;
import org.geogig.server.model.AuthUser;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class TransactionServiceEventsAspect extends AbstractServiceEventAspect {

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.transactionBeginExecution()", returning = "transaction")
    public void afterTransactionBegin(JoinPoint joinPoint, Transaction transaction) {
        Created event = TransactionEvent.Created.builder().transaction(transaction).build();
        publishEvent(event);
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.transactionDeleteExecution()", returning = "transaction")
    public void afterTransactionDeleted(JoinPoint joinPoint, @Nullable Transaction transaction) {
        if (transaction != null) {
            Deleted event = TransactionEvent.Deleted.builder().transaction(transaction).build();
            publishEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    @Around("org.geogig.server.events.aop.PointCuts.transactionCommitExecution()")
    public CompletableFuture<Transaction> aroundTransactionCommit(ProceedingJoinPoint joinPoint)
            throws Throwable {

        final User caller = (User) joinPoint.getArgs()[0];
        final Transaction transaction = (Transaction) joinPoint.getArgs()[1];
        CompletableFuture<Transaction> future;

        future = (CompletableFuture<Transaction>) joinPoint.proceed();

        future.whenComplete((t, ex) -> {

            Optional<AuthUser> authUser = auth.getUser(caller.getId());
            CommittedBuilder<?, ?> builder = TransactionEvent.Committed.builder().caller(authUser);
            if (ex == null) {
                builder.success(true);
                builder.transaction(t);
            } else {
                builder.success(false);
                builder.transaction(transaction);
            }
            Committed event = builder.build();
            publishEvent(event);
        });
        return future;
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.transactionAbortExecution()", returning = "transaction")
    public void afterTransactionAborted(JoinPoint joinPoint, Transaction transaction) {

        Aborted event = TransactionEvent.Aborted.builder().transaction(transaction)
                .caller(auth.getCurrentUser()).build();
        publishEvent(event);
    }
}
