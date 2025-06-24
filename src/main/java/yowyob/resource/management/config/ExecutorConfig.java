package yowyob.resource.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class ExecutorConfig {

    @Value("${app.executor.service.core-pool-size:10}")
    private int serviceExecutorCorePoolSize;

    @Value("${app.executor.service.max-pool-size:20}")
    private int serviceExecutorMaxPoolSize;

    @Value("${app.executor.resource.core-pool-size:10}")
    private int resourceExecutorCorePoolSize;

    @Value("${app.executor.resource.max-pool-size:20}")
    private int resourceExecutorMaxPoolSize;

    @Value("${app.executor.queue-capacity:1000}")
    private int queueCapacity;

    @Bean(name = "serviceActionExecutorPool")
    public ThreadPoolTaskExecutor serviceActionExecutorPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(serviceExecutorCorePoolSize);
        executor.setMaxPoolSize(serviceExecutorMaxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ServiceAction-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "resourceActionExecutorPool")
    public ThreadPoolTaskExecutor resourceActionExecutorPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(resourceExecutorCorePoolSize);
        executor.setMaxPoolSize(resourceExecutorMaxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ResourceAction-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}