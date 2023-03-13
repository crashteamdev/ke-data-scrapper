package dev.crashteam.ke_data_scrapper.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int threads = processors * (1 + 5000 / 500);
        executor.setCorePoolSize(threads / 2);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("data-executor-");
        executor.initialize();
        return executor;
    }
}