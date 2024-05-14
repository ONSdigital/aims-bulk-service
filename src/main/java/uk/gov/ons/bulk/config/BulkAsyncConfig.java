package uk.gov.ons.bulk.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequest;

@Slf4j
@Configuration
public class BulkAsyncConfig implements AsyncConfigurer {

    @Value("${async.executor.thread.core_pool_size}")
    private int corePoolSize;
    @Value("${async.executor.thread.max_pool_size}")
    private int maxPoolSize;
    @Value("${async.executor.thread.queue_capacity}")
    private int queueCapacity;
    @Value("${async.executor.thread.keep_alive_seconds}")
    private int keepAliveSeconds;
    @Value("${async.executor.thread.name.prefix}")
    private String namePrefix;

    @Bean(name = "asyncServiceExecutor")
    public Executor asyncServiceExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Set the number of core threads
        executor.setCorePoolSize(corePoolSize);
        // Set the maximum number of threads. Threads exceeding the number of core threads will be applied only after the buffer queue is full
        // executor.setMaxPoolSize(maxPoolSize); Allow Default Integer.MAX_VALUE
        // Set buffer queue size
        // executor.setQueueCapacity(queueCapacity); Allow Default Integer.MAX_VALUE
        // Set the maximum idle time of threads. Threads that exceed the number of core threads will be destroyed after the idle time arrives
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // Set the prefix of thread name. After setting, it is convenient for us to locate the thread pool where the processing task is located
        executor.setThreadNamePrefix(namePrefix);
        // Set rejection policy: how to handle new tasks when the thread pool reaches the maximum number of threads
        // CALLER_RUNS: when adding to the thread pool fails, the main thread will execute this task by itself,
        // When the thread pool has no processing capacity, the policy will directly run the rejected task in the calling thread of the execute method; If the executor is closed, the task is discarded
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Thread pool initialization
        executor.initialize();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncServiceExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error(String.format("Exception with message : %s", ex.getMessage()));
            log.error(String.format("Method : %s", method.toString()));
            log.error(String.format("Number of parameters : %s", params.length));
            for (Object param : params) {
                log.error(String.format("Parameter value : %s", param));
            }
        };
    }
}
