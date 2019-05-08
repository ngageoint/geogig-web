package org.geogig.server.stats.storage;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = StatsStorageConfiguration.class, lazyInit = true)
@EntityScan(basePackageClasses = StatsStorageConfiguration.class)
@EnableJpaRepositories(//
//        basePackageClasses = { StatsStorageConfiguration.class }//
//        , entityManagerFactoryRef = "userEntityManagerFactory"//
//        , transactionManagerRef = "userTransactionManager"//
)
@EnableJpaAuditing
public class StatsStorageConfiguration {
    //@formatter:off
    /* REVISIT: set up separate databases
    @Bean(name = "userDataSource")
    @ConfigurationProperties(prefix = "stats.datasource")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "userEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("userDataSource") DataSource userDataSource) {

        return builder.dataSource(userDataSource).packages(StatsStorageConfiguration.class)
                .persistenceUnit("stats").build();
    }

    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory userEntityManagerFactory) {

        return new JpaTransactionManager(userEntityManagerFactory);
    }
    */
    //@formatter:on
}
