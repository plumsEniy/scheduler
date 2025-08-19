import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.github.rholder.retry.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @description: Onretry的测试
 * @Date: 2024/3/29 15:53
 * @Author: nizhiqiang
 */


@Slf4j
public class OnRetryTest {

    public static void main(String[] args) throws ExecutionException, RetryException {
        onRetryTest();
    }


    public static void onRetryTest() throws ExecutionException, RetryException {

        final int maxAttemptNumber = 3;
        Retryer<Boolean> retry = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                // 运行时异常时
                .retryIfExceptionOfType(TaskEventHandleException.class)
                .retryIfRuntimeException() // callable抛出RuntimeException重试
                // call方法返回false时重试
                .retryIfResult(result -> Objects.equals(result, false))
                // 10秒后重试
                .withWaitStrategy(WaitStrategies.fixedWait(3, TimeUnit.SECONDS))
                .withRetryListener(new RetryListener() {

                    @Override
                    public <Boolean> void onRetry(Attempt<Boolean> attempt) {
                        Boolean result = attempt.getResult();
                        long attemptNumber = attempt.getAttemptNumber();
                        log.info("retry execute, attemptNumber:{}, result is {}", attemptNumber, result);
                        if (attempt.hasException()) {
                            Throwable exceptionCause = attempt.getExceptionCause();
                            String message = exceptionCause.getMessage();
                            String format = String.format(" retry exception:%s,attemptNumber:%s", message, attemptNumber);
                            log.warn(format);
                        }

                        if (attemptNumber >= maxAttemptNumber) {
                            String format = String.format(" retry overtime, retry time is %s, result is %s", attemptNumber, result);
                            log.warn(format);

                        }
                    }
                })
                // 重试n次，超过次数就...
                .withStopStrategy(StopStrategies.stopAfterAttempt(maxAttemptNumber))
                .build();
        retry.call(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return false;
            }
        });
    }
}
