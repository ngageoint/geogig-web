package org.geogig.server.events.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.geogig.server.events.model.UserEvent;
import org.geogig.server.model.User;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class UserServiceEventsAspect extends AbstractServiceEventAspect {

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.userCreateExecution()", returning = "user")
    public void afterUserCreated(JoinPoint joinPoint, User user) {
        publishEvent(UserEvent.Created.builder().user(user).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.userModifyExecution()", returning = "user")
    public void afterUserModified(JoinPoint joinPoint, User user) {
        publishEvent(UserEvent.Updated.builder().user(user).build());
    }

    @AfterReturning(pointcut = "org.geogig.server.events.aop.PointCuts.userDeleteExecution()", returning = "user")
    public void afterUserDeleted(JoinPoint joinPoint, User user) {
        publishEvent(UserEvent.Deleted.builder().user(user).build());
    }

}
