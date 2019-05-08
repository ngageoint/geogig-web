package org.geogig.server.service.async;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "org.geogig.server.service" }, lazyInit = true)
public class AsyncTasksServiceTestConfiguration {

}
