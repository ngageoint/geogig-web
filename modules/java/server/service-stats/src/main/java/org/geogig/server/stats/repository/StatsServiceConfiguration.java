package org.geogig.server.stats.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = StatsServiceConfiguration.class, lazyInit = true)
public class StatsServiceConfiguration {

}
