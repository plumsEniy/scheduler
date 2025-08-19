package com.bilibili.cluster.scheduler.common.dto.parameters.dto.node.monitor;

import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import lombok.Data;

import java.util.List;

/**
 * @description:监控节点参数
 * @Date: 2024/9/4 19:50
 * @Author: nizhiqiang
 */

@Data
public class MonitorNodeParams {

    /**
     * 需要移除的监控
     */
    List<MetricNodeInstance> removeMonitorInstanceList;

    /**
     * 新增的监控
     */
    List<MetricNodeInstance> addMonitorInstanceList;

    MetricEnvEnum monitorEnv;

    String token;

    Integer integrationId;
}
