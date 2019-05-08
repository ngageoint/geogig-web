package org.geogig.server.service.pr;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = PullRequestServiceConfiguration.class, lazyInit = true)
@EntityScan(basePackageClasses = PullRequestServiceConfiguration.class)
@EnableJpaRepositories(basePackageClasses = PullRequestServiceConfiguration.class)
@EnableJpaAuditing
public class PullRequestServiceConfiguration {

}