package org.geogig.server.service;

import org.geogig.server.geogig.GeogigRepositoryProvider;
import org.geogig.server.model.AuditedEntity;
import org.geogig.server.service.repositories.RepositoriesJPAStore;
import org.geogig.server.service.stores.StoresJPAStore;
import org.geogig.server.service.user.UsersJPAStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//@formatter:off
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {GeogigRepositoryProvider.class, AuthServiceAuditorResolver.class, StoresJPAStore.class, UsersJPAStore.class, RepositoriesJPAStore.class }, lazyInit = true)
@EntityScan(basePackageClasses = { AuditedEntity.class })
@EnableJpaRepositories(basePackageClasses = { StoresJPAStore.class, UsersJPAStore.class, RepositoriesJPAStore.class })
@EnableJpaAuditing(auditorAwareRef = "auditorResolver")
//@formatter:on
public class ConfigServiceConfig {

    public @Bean(name = "auditorResolver") AuditorAware<String> auditorResolver() {
        return new AuthServiceAuditorResolver();
    }
}
