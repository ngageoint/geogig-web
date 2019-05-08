package org.geogig.server.service.branch;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = BranchAdminServiceConfiguration.class, lazyInit = true)
@EntityScan(basePackageClasses = BranchAdminServiceConfiguration.class)
@EnableJpaRepositories(basePackageClasses = BranchAdminServiceConfiguration.class)
public class BranchAdminServiceConfiguration {

}
