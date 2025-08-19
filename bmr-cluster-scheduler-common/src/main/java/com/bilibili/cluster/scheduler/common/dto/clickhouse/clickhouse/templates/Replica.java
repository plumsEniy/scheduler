package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/24 14:19
 * @Author: nizhiqiang
 */

@Data
public class Replica {

    String name;

    ShardTemplate templates;

}
