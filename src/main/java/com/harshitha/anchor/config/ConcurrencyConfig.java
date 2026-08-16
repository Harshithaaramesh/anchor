package com.harshitha.anchor.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verification and retrieval work is I/O-bound (LLM calls, vector store queries), so a
 * virtual-thread-per-task executor lets us fan out per-claim checks and the
 * retrieval/generation calls concurrently without tuning a fixed thread pool size.
 */
@Configuration
public class ConcurrencyConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService verificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
