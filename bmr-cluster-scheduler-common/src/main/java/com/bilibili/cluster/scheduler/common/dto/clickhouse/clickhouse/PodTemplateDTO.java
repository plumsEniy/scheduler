package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile.ConfigContainerResource;
import lombok.Data;

/**
 * @description: pod模版信息
 * @Date: 2025/2/8 14:09
 * @Author: nizhiqiang
 */

@Data
public class PodTemplateDTO {

    private String templateName;

    private String image;

    ConfigContainerResource requests;

    ConfigContainerResource limits;
}
