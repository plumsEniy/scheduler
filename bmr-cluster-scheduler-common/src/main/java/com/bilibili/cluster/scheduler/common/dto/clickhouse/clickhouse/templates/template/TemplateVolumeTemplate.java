package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/2/5 14:54
 * @Author: nizhiqiang
 */

@Data
public class TemplateVolumeTemplate {
    String name;

    @Alias("storage_class")
    String storageClass;

    TemplateVolumeResource resources;
}
