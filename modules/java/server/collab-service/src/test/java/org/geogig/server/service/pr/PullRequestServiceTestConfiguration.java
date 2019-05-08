package org.geogig.server.service.pr;

import org.geogig.server.test.ConfigTestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ ConfigTestConfiguration.class })
@EnableAutoConfiguration
@ComponentScan(basePackages = { "org.geogig.server.service" }, lazyInit = true)
public class PullRequestServiceTestConfiguration {

}
