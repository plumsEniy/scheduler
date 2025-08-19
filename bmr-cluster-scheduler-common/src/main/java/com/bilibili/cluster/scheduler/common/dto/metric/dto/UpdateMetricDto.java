package com.bilibili.cluster.scheduler.common.dto.metric.dto;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: 新增 删除 全量更新采集任务的dto
 * @Date: 2024/8/30 11:55
 * @Author: nizhiqiang
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMetricDto {

    /**
     * 集成任务id
     */
    @Alias("integration_id")
    private Integer integrationId;
    private List<MetricNodeInstance> instances;
}
