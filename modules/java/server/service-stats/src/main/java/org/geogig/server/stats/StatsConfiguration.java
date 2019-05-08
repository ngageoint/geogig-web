package org.geogig.server.stats;

import org.geogig.server.stats.repository.StatsServiceConfiguration;
import org.geogig.server.stats.storage.StatsStorageConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = StatsService.class, lazyInit = true)
@Import({ StatsStorageConfiguration.class, StatsServiceConfiguration.class })
public class StatsConfiguration {

}
