package com.bilibili.cluster.scheduler.common.dto.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 监控所需属性
 * @Date: 2024/9/5 15:35
 * @Author: nizhiqiang
 */

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class MetricHostInfo {

    private String hostName;
    private String ip;
    private String rack;

}
