package org.geogig.server.events;

import org.geogig.server.events.aop.PointCuts;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackageClasses = { PointCuts.class }, lazyInit = true)
public class EventsConfiguration {

}
