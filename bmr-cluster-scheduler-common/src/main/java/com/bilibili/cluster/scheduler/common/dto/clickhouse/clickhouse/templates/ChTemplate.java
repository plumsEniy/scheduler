package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile.FileServiceTemplate;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplatePodTemplate;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplateVolumeTemplate;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2025/2/7 16:49
 * @Author: nizhiqiang
 */

@Data
public class ChTemplate {

    /**
     * podTemplates.yaml文件
     */
    @Alias("pod_templates")
    List<TemplatePodTemplate> podTemplates;

    /**
     * serviceTemplates.yaml文件
     */
    FileServiceTemplate service;

    /**
     * volumeClaimTemplates.yaml
     */
    @Alias("volume_claim_templates")
    List<TemplateVolumeTemplate> volumeTemplate;

}
