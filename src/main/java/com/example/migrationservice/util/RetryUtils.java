package com.example.migrationservice.util;

import java.time.Duration;
import java.util.concurrent.Callable;

public final class RetryUtils {

    private RetryUtils() {
    }

    public static <T> T callWithRetry(String operationName, int maxAttempts, Duration delay, Callable<T> callable) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception exception) {
                lastException = exception;
                if (attempt == maxAttempts) {
                    break;
                }
                sleep(delay);
            }
        }

        throw new IllegalStateException("Operation failed after retries: " + operationName, lastException);
    }

    public static void runWithRetry(String operationName, int maxAttempts, Duration delay, Runnable runnable) {
        callWithRetry(operationName, maxAttempts, delay, () -> {
            runnable.run();
            return null;
        });
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }
}
