package com.bilibili.cluster.scheduler.api.service.cache;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public interface CacheService {

    <K, V> V getOrComputeWithFixedRate(CacheType cacheType, K key, Function<K, V> f, long interval, TimeUnit unit) throws ExecutionException;

    <K, V> V getImmediately(CacheType cacheType, K key, Class<V> vClass);

    ExecutionFlowEntity getFlowEntity(long flowId) throws Exception;

}
