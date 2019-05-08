package org.geogig.server.events.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.geogig.server.events.model.StoreEvent;
import org.geogig.server.model.Store;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class StoreServiceEventsAspect extends AbstractServiceEventAspect {

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.storeCreateExecution()", returning = "store")
    public void afterStoreCreated(JoinPoint joinPoint, Store store) {
        publishEvent(StoreEvent.Created.builder().store(store).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.storeModifyExecution()", returning = "store")
    public void afterStoreModified(JoinPoint joinPoint, Store store) {
        publishEvent(StoreEvent.Updated.builder().store(store).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.storeDeleteExecution()", returning = "store")
    public void afterStoreDeleted(JoinPoint joinPoint, Store store) {
        publishEvent(StoreEvent.Deleted.builder().store(store).build());
    }

}
