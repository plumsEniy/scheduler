package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import lombok.Data;

/**
 * @description: ck容器的全局配置参数
 * @Date: 2025/1/24 11:20
 * @Author: nizhiqiang
 */
@Data
public class PaasConfig {

    String env;

    String resourcePoolName;
}
