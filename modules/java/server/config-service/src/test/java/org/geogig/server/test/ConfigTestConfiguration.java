package org.geogig.server.test;

import org.geogig.server.service.ConfigServiceConfig;
import org.geogig.server.service.auth.AuthenticationService;
import org.geogig.server.test.security.TestSecurityConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAutoConfiguration
@Import({ ConfigServiceConfig.class, TestSecurityConfig.class })
@ComponentScan(basePackageClasses = { AuthenticationService.class }, lazyInit = true)
public class ConfigTestConfiguration {

    public @Bean ServiceTestSupport serviceTestSupport() {
        return new ServiceTestSupport();
    }

    public @Bean(name = "forksExecutor") TaskExecutor forksExecutor() {
        int corePoolSize = 8;
        int maxPoolSize = 8;
        int queueCapacity = 10;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("geogig-repository-forks-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

    public @Bean(name = "repoCommandsExecutor") TaskExecutor rpcExecutor() {
        int corePoolSize = 8;
        int maxPoolSize = 8;
        int queueCapacity = 10;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("geogig-rpc-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

}
