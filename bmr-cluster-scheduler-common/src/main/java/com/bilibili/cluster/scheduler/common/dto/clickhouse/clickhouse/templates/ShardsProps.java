package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

/**
 * @description: 对应cluster_replica_props.xml 和 cluster_admin_props.xml
 * @Date: 2025/1/24 14:40
 * @Author: nizhiqiang
 */

@Data
public class ShardsProps {

    String secret;

    /**
     * 新增和迭代默认使用的配置
     */
    String dataVolumeClaimTemplate;

    /**
     * 新增和迭代默认使用的配置
     */
    String replicaServiceTemplate;

    String schemaPolicyReplica;

    String schemaPolicyShard;

    String name;
}
