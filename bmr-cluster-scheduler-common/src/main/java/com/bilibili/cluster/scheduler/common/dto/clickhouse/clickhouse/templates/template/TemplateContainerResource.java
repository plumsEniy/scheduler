package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/24 16:14
 * @Author: nizhiqiang
 */

@Data
public class TemplateContainerResource {

    @Alias("cpu_req")
    private Integer cpuReq;

    @Alias("cpu_limit")
    private Integer cpuLimit;

    @Alias("mem_limit")
    private Integer memLimit;

    @Alias("mem_req")
    private Integer memReq;
}
