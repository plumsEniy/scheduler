package com.bilibili.cluster.scheduler.common.dto.metric.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 监控配置
 * @Date: 2024/4/17 14:10
 * @Author: nizhiqiang
 */
@Data
@EqualsAndHashCode
public class MetricTokenConfig {

    String component;

    Integer integrationId;

    String token;
}
