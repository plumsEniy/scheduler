package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import lombok.Data;

/**
 * @description: 对应podTemplates.yaml文件
 * @Date: 2025/1/20 20:03
 * @Author: nizhiqiang
 */

@Data
public class FilePodTemplate {
    String name;

    ConfigPodSpec spec;
}
