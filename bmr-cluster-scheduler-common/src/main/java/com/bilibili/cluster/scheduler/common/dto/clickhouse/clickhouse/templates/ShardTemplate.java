package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/24 14:23
 * @Author: nizhiqiang
 */

@Data
public class ShardTemplate {
    String podTemplate;

    String dataVolumeClaimTemplate;

    String replicaServiceTemplate;
}
