package com.bilibili.cluster.scheduler.api.service.metric;

import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.UpdateMetricDto;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;

import java.util.List;

/**
 * @description: monitor服务
 * @Date: 2024/8/30 11:46
 * @Author: nizhiqiang
 */
public interface MetricService {

    /**
     * 新增采集对象，一次上限5w条
     * @param env
     * @param updateMonitor
     * @param token
     */
    void addMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token);

    /**
     * 删除采集对象，一次上限5w条
     * @param env
     * @param updateMonitor
     * @param token
     */
    void delMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token);

    /**
     * 全量更新采集对象，一次上限20w条
     * @param env
     * @param updateMonitor
     * @param token
     */
    void fullUpdateMetricInstance(MetricEnvEnum env, UpdateMetricDto updateMonitor, String token);

    /**
     * 获取全量instance
     * @param env
     * @param integrationId
     * @param token
     * @return
     */
    List<MetricNodeInstance> queryMetricInstanceList(MetricEnvEnum env, Integer integrationId);

}
