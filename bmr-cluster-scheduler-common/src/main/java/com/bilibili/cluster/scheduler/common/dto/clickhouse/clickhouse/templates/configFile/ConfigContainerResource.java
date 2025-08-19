package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import lombok.Data;

@Data
public class ConfigContainerResource {
    String memory;

    String cpu;
}