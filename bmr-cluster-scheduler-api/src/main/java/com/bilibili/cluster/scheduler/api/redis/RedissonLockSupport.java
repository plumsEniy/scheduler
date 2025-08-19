package com.bilibili.cluster.scheduler.api.redis;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedissonLockSupport {

    private List<String> lockKeys = new ArrayList<>();

    @Resource
    RedissonClient redissonClient;

    public Boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        final int randomInt = RandomUtil.getRandom().nextInt(1_00);
        ThreadUtil.safeSleep(randomInt);
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = lock.tryLock(waitTime, leaseTime, unit);
            if (locked) lockKeys.add(lockKey);
            return locked;
        } catch (InterruptedException | CancellationException e) {
            log.error("尝试获取锁 {} 失败", lockKey);
        }
        return Boolean.FALSE;
    }

    public boolean unLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (null != lock && lock.isHeldByCurrentThread()) { //判断锁是否存在，和是否当前线程加的锁。
                lock.unlock();
                return lockKeys.remove(lockKey);
            }
        } catch (Exception e) {
            log.error("解锁 {} 失败", lockKey);
        }
        return false;
    }

    public Boolean tryLockAsync(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            RFuture<Boolean> locked = lock.tryLockAsync(waitTime, leaseTime, unit);
            if (locked.get()) lockKeys.add(lockKey);
            return locked.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("尝试获取Async锁 {} 失败", lockKey);
        }
        return Boolean.FALSE;
    }

    public boolean unAsyncLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (null != lock && lock.isHeldByCurrentThread()) { //判断锁是否存在，和是否当前线程加的锁。
                RFuture<Void> future = lock.unlockAsync();
                if (future.await(5 * 1000) && future.isSuccess()) {
                    return lockKeys.remove(lockKey);
                }
            }
        } catch (Exception e) {
            log.error("解Async锁 {} 失败", lockKey);
        }
        return false;
    }


}
