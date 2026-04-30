package kosukeroku.ms_account_reservation.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AsyncProperties.class)
public class ThreadPoolConfig {

    private final AsyncProperties properties;
    private final MeterRegistry meterRegistry;

    @Bean(name = "reportTaskExecutor")
    public ThreadPoolTaskExecutor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        meterRegistry.gauge("async.pool.active.threads", executor, ThreadPoolTaskExecutor::getActiveCount);
        meterRegistry.gauge("async.pool.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize);
        meterRegistry.gauge("async.pool.core.pool.size", executor, ThreadPoolTaskExecutor::getCorePoolSize);
        meterRegistry.gauge("async.pool.max.pool.size", executor, ThreadPoolTaskExecutor::getMaxPoolSize);
        meterRegistry.gauge("async.pool.queue.size", executor, ThreadPoolTaskExecutor::getQueueSize);

        return executor;
    }
}