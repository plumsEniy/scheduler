package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import lombok.Data;

/**
 * @description: volumeClaimTemplates.yaml文件
 * @Date: 2025/1/24 11:13
 * @Author: nizhiqiang
 */
@Data
public class FileVolumeTemplate {
    String name;

    ConfigVolumeSpec spec;
}
