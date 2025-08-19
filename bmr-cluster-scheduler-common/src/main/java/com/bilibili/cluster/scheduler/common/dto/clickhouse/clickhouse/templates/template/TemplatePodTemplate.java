package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.DnsConfig;
import lombok.Data;

/**
 * @description:模版中的podtemplate
 * @Date: 2025/1/24 16:00
 * @Author: nizhiqiang
 */
@Data
public class TemplatePodTemplate {
    String image;

    TemplateContainerResource resources;

    @Alias("dns_config")
    DnsConfig dnsConfig;

    String name;
}
