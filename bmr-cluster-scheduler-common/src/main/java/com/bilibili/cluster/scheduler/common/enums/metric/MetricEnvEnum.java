package com.bilibili.cluster.scheduler.common.enums.metric;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 监控枚举
 * @Date: 2024/8/30 14:19
 * @Author: nizhiqiang
 */


@AllArgsConstructor
public enum MetricEnvEnum {
    UAT("http://uat-cloud.bilibili.co/metrics/api"),
    PROD("http://cloud.bilibili.co/metrics/api"),
    ;

    @Getter
    private String url;
}
