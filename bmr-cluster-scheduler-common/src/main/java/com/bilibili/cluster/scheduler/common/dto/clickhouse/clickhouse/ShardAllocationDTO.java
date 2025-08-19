package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse;

import lombok.Data;

import java.util.List;

/**
 * @description: shard分批dto
 * @Date: 2025/3/18 16:35
 * @Author: nizhiqiang
 */
@Data
public class ShardAllocationDTO {

    List<Integer> replicaAllocationList;

    List<Integer> adminAllocationList;
}
