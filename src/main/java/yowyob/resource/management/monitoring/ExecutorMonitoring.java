package yowyob.resource.management.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;


import java.util.concurrent.ThreadPoolExecutor;

@Component
@ConditionalOnProperty(name = "app.executor.monitoring.enabled", havingValue = "true")
public class ExecutorMonitoring {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorMonitoring.class);

    @Autowired(required = false)
    private ThreadPoolTaskExecutor serviceActionExecutorPool;

    @Autowired(required = false)
    private ThreadPoolTaskExecutor resourceActionExecutorPool;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${app.executor.monitoring.log-interval:60000}")
    private long logInterval;

    private Counter serviceActionsExecuted;
    private Counter resourceActionsExecuted;
    private Timer serviceActionExecutionTime;
    private Timer resourceActionExecutionTime;

    @PostConstruct
    public void initMetrics() {
        // Counters for executed actions
        serviceActionsExecuted = Counter.builder("service.actions.executed")
                .description("Number of service actions executed")
                .register(meterRegistry);

        resourceActionsExecuted = Counter.builder("resource.actions.executed")
                .description("Number of resource actions executed")
                .register(meterRegistry);

        // Timers for execution time
        serviceActionExecutionTime = Timer.builder("service.action.execution.time")
                .description("Service action execution time")
                .register(meterRegistry);

        resourceActionExecutionTime = Timer.builder("resource.action.execution.time")
                .description("Resource action execution time")
                .register(meterRegistry);

        // Gauges for thread pool metrics using MeterRegistry.gauge() method
        if (serviceActionExecutorPool != null) {
            meterRegistry.gauge("service.executor.active.threads", 
                serviceActionExecutorPool, 
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getActiveCount();
                    } catch (Exception e) {
                        logger.warn("Error getting active thread count for service executor: {}", e.getMessage());
                        return 0;
                    }
                });

            meterRegistry.gauge("service.executor.queue.size", 
                serviceActionExecutorPool,
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getQueue().size();
                    } catch (Exception e) {
                        logger.warn("Error getting queue size for service executor: {}", e.getMessage());
                        return 0;
                    }
                });

            meterRegistry.gauge("service.executor.pool.size", 
                serviceActionExecutorPool,
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getPoolSize();
                    } catch (Exception e) {
                        logger.warn("Error getting pool size for service executor: {}", e.getMessage());
                        return 0;
                    }
                });
        }

        if (resourceActionExecutorPool != null) {
            meterRegistry.gauge("resource.executor.active.threads", 
                resourceActionExecutorPool,
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getActiveCount();
                    } catch (Exception e) {
                        logger.warn("Error getting active thread count for resource executor: {}", e.getMessage());
                        return 0;
                    }
                });

            meterRegistry.gauge("resource.executor.queue.size", 
                resourceActionExecutorPool,
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getQueue().size();
                    } catch (Exception e) {
                        logger.warn("Error getting queue size for resource executor: {}", e.getMessage());
                        return 0;
                    }
                });

            meterRegistry.gauge("resource.executor.pool.size", 
                resourceActionExecutorPool,
                executor -> {
                    try {
                        return executor.getThreadPoolExecutor().getPoolSize();
                    } catch (Exception e) {
                        logger.warn("Error getting pool size for resource executor: {}", e.getMessage());
                        return 0;
                    }
                });
        }

        logger.info("Executor monitoring initialized");
    }

    @Scheduled(fixedDelayString = "${app.executor.monitoring.log-interval:60000}")
    public void logExecutorStats() {
        if (serviceActionExecutorPool != null) {
            ThreadPoolExecutor serviceExecutor = serviceActionExecutorPool.getThreadPoolExecutor();
            logger.info("Service Executor Stats - Active: {}, Pool Size: {}, Queue Size: {}, Completed: {}",
                    serviceExecutor.getActiveCount(),
                    serviceExecutor.getPoolSize(),
                    serviceExecutor.getQueue().size(),
                    serviceExecutor.getCompletedTaskCount());

            // Alert if queue is getting full
            if (serviceExecutor.getQueue().size() > serviceExecutor.getQueue().remainingCapacity() * 0.8) {
                logger.warn("Service Executor queue is {}% full! Consider increasing pool size or queue capacity",
                        (serviceExecutor.getQueue().size() * 100) / 
                        (serviceExecutor.getQueue().size() + serviceExecutor.getQueue().remainingCapacity()));
            }
        }

        if (resourceActionExecutorPool != null) {
            ThreadPoolExecutor resourceExecutor = resourceActionExecutorPool.getThreadPoolExecutor();
            logger.info("Resource Executor Stats - Active: {}, Pool Size: {}, Queue Size: {}, Completed: {}",
                    resourceExecutor.getActiveCount(),
                    resourceExecutor.getPoolSize(),
                    resourceExecutor.getQueue().size(),
                    resourceExecutor.getCompletedTaskCount());

            // Alert if queue is getting full
            if (resourceExecutor.getQueue().size() > resourceExecutor.getQueue().remainingCapacity() * 0.8) {
                logger.warn("Resource Executor queue is {}% full! Consider increasing pool size or queue capacity",
                        (resourceExecutor.getQueue().size() * 100) / 
                        (resourceExecutor.getQueue().size() + resourceExecutor.getQueue().remainingCapacity()));
            }
        }
    }

    public void recordServiceActionExecution() {
        serviceActionsExecuted.increment();
    }

    public void recordResourceActionExecution() {
        resourceActionsExecuted.increment();
    }

    public Timer.Sample startServiceActionTimer() {
        return Timer.start(meterRegistry);
    }

    public Timer.Sample startResourceActionTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopServiceActionTimer(Timer.Sample sample) {
        sample.stop(serviceActionExecutionTime);
    }

    public void stopResourceActionTimer(Timer.Sample sample) {
        sample.stop(resourceActionExecutionTime);
    }
}