package com.bilibili.cluster.scheduler.api.service.redis;

import com.bilibili.cluster.scheduler.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * redis操作Service的实现类
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    @Resource
    private RedisTemplate<String, String> stringRedisTemplate;

    @Override
    public void set(String key, String value) {
        if (StringUtils.isBlank(value)) return;
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("set redis key: {}, value: {} error, case by {}", key, value, e.getMessage(), e);
        }
    }

    @Override
    public void set(String key, String value, long expire) {
        if (StringUtils.isBlank(value)) return;
        try {
            stringRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("set redis key: {}, value: {}, expire: {} MILLISECONDS error, case by {}",
                    key, value, expire, e.getMessage(), e);
        }
    }

    @Override
    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("query redis key: {} error, case by {}", key, e.getMessage(), e);
            return Constants.EMPTY_STRING;
        }
    }

    @Override
    public boolean expire(String key, long time) {
        try {
            return stringRedisTemplate.expire(key, time, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("expire redis key: {} error, case by {}", key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void remove(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.error("remove redis key: {} error, case by {}", key, e.getMessage(), e);
        }
    }

    @Override
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }
}

