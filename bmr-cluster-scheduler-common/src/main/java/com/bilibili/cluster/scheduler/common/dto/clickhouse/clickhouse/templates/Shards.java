package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

import java.util.List;

/**
 * @description: 对应cluster_admin_props.xml 和 cluster_replica_shards.yaml 对应实际容器配置
 * @Date: 2025/1/24 11:45
 * @Author: nizhiqiang
 */

@Data
public class Shards {
    String name;

    Integer replicasCount;

    List<Replica> replicas;
}
