package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/24 11:14
 * @Author: nizhiqiang
 */

@Data
public class ConfigVolumeSpec {
    String storageClassName;

    ConfigVolumeResource resources;

    @Data
    public static class ConfigVolumeResource {
        com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile.ConfigStorageResource requests;
    }
}
