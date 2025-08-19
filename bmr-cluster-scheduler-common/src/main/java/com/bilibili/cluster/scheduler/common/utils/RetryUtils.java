package com.bilibili.cluster.scheduler.common.utils;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class RetryUtils {

    private static final Long TIME_WAIT_SECONDS= 1l;

    public static <V> Retryer<V> getRetryer(int retry, int interval) {
        return RetryerBuilder.<V>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                .retryIfRuntimeException().withStopStrategy(StopStrategies.stopAfterAttempt(retry))
                .withWaitStrategy(WaitStrategies.fixedWait(
                        interval > 0 ? interval : TIME_WAIT_SECONDS, TimeUnit.SECONDS))
                .build();
    }

    /**
     *
     * @param retry 重试次数
     * @param timeInterval 重试间隔 秒
     * @param callable action
     * @param <V> result
     * @return
     * @throws Exception
     */
    public static <V> V retryWith(int retry, int timeInterval, Callable<V> callable) throws Exception {
        final Retryer<V> retryer = getRetryer(retry, timeInterval);
        return retryer.call(callable);
    }


    private static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy(3, 5000L);

    /**
     * Retry to execute the given function with the default retry policy.
     */
    public static <T> T retryFunction(@NonNull Supplier<T> supplier) {
        return retryFunction(supplier, DEFAULT_RETRY_POLICY);
    }

    /**
     * Retry to execute the given function with the given retry policy, the retry policy is used to defined retryTimes and retryInterval.
     * This method will sleep for retryInterval when execute given supplier failure.
     */
    public static <T> T retryFunction(@NonNull Supplier<T> supplier, @NonNull RetryPolicy retryPolicy) {
        int retryCount = 0;
        long retryInterval = 0L;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception ex) {
                if (retryCount == retryPolicy.getMaxRetryTimes()) {
                    throw ex;
                }
                retryCount++;
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("The current thread is interrupted, will stop retry", e);
                }
            }
        }
    }

    @Data
    public static final class RetryPolicy {

        /**
         * The max retry times
         */
        private final int maxRetryTimes;
        /**
         * The retry interval, if the give function is failed, will sleep the retry interval milliseconds and retry again.
         */
        private final long retryInterval;

    }

}

