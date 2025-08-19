package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/2/7 17:13
 * @Author: nizhiqiang
 */

@Data
public class TemplateSchemaPolicy {
    String replica;

    String shard;
}
