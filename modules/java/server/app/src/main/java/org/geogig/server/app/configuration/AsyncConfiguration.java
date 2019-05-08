package org.geogig.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Autowired
    private Environment env;

    public @Bean(name = "forksExecutor") TaskExecutor forksExecutor() {
        int corePoolSize = env.getProperty("forksExecutor.corePoolSize", Integer.class, 1);
        int maxPoolSize = env.getProperty("forksExecutor.maxPoolSize", Integer.class, 5);
        int queueCapacity = env.getProperty("forksExecutor.queueCapacity", Integer.class, 20);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // executor.setBeanName("forksExecutor");
        executor.setThreadNamePrefix("geogig-repository-forks-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

    public @Bean(name = "immediateTasksExecutor") TaskExecutor immediateTasksExecutor() {
        int corePoolSize = env.getProperty("forksExecutor.corePoolSize", Integer.class, 1);
        int maxPoolSize = env.getProperty("forksExecutor.maxPoolSize", Integer.class, 8);
        int queueCapacity = env.getProperty("forksExecutor.queueCapacity", Integer.class, 40);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // executor.setBeanName("immediateTasksExecutor");
        executor.setThreadNamePrefix("geogig-immediate-tasks-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

    public @Bean(name = "repoCommandsExecutor") TaskExecutor repoCommandsExecutor() {
        int corePoolSize = env.getProperty("forksExecutor.corePoolSize", Integer.class, 1);
        int maxPoolSize = env.getProperty("forksExecutor.maxPoolSize", Integer.class, 8);
        int queueCapacity = env.getProperty("forksExecutor.queueCapacity", Integer.class, 40);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // executor.setBeanName("immediateTasksExecutor");
        executor.setThreadNamePrefix("geogig-command-runner-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }

    public @Bean(name = "externalServicesCallsExecutor") TaskExecutor externalServicesCallsExecutor() {
        int corePoolSize = 1;// env.getProperty("forksExecutor.corePoolSize", Integer.class, 1);
        int maxPoolSize = 24;// env.getProperty("forksExecutor.maxPoolSize", Integer.class, 24);
        int queueCapacity = 120;// env.getProperty("forksExecutor.queueCapacity", Integer.class,
                                // 120);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // executor.setBeanName("immediateTasksExecutor");
        executor.setThreadNamePrefix("external-services-calls-executor-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        return executor;
    }
}
