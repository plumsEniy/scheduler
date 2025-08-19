package com.bilibili.cluster.scheduler.api.service.cache;

import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.exception.LocalCacheException;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Resource
    ExecutionFlowService flowService;

    private ConcurrentHashMap<CacheType, LoadingCache> cacheStore = new ConcurrentHashMap(16);

    ExecutorService threadPoolService = ThreadUtils.newDaemonFixedThreadExecutor("scheduler-cache-thread", 2);

    @Override
    public <K, V> V getOrComputeWithFixedRate(CacheType cacheType, K key, Function<K, V> f, long interval, TimeUnit unit) throws ExecutionException {
        LoadingCache<K, V> loadingCache = cacheStore.get(cacheType);
        if (Objects.isNull(loadingCache)) {
            log.info("start init cache type {}. function is {}", cacheType, f);
            loadingCache = CacheBuilder.newBuilder().maximumSize(1000).refreshAfterWrite(interval, unit)
                    .build(new CacheLoader<K, V>() {
                        @Override
                        public V load(K key) throws Exception {
                            return f.apply(key);
                        }
                        @Override
                        public ListenableFuture<V> reload(K key, V oldValue) throws Exception {
                            ListenableFutureTask<V> task =
                                    ListenableFutureTask.create(() -> f.apply(key));
                            threadPoolService.submit(task);
                            return task;
                        }
                    });
            cacheStore.putIfAbsent(cacheType, loadingCache);
        }
        return loadingCache.get(key);
    }

    @Override
    public <K, V> V getImmediately(CacheType cacheType, K key, Class<V> vClass) {
        LoadingCache<K, V> loadingCache = cacheStore.get(cacheType);
        Assert.notNull(loadingCache,  "Uninitialized with cache type: " + cacheType);
        try {
            return loadingCache.get(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new LocalCacheException(e);
        }
    }

    @Override
    public ExecutionFlowEntity getFlowEntity(long flowId) throws Exception {
        return getOrComputeWithFixedRate(CacheType.EXECUTE_DATA, flowId, flowService::getById, 10, TimeUnit.SECONDS);
    }
}
