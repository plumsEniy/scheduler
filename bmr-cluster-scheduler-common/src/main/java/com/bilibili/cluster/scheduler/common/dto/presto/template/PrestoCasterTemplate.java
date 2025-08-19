package com.bilibili.cluster.scheduler.common.dto.presto.template;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/7/10 17:49
 * @Author: nizhiqiang
 */

@Data
public class PrestoCasterTemplate {
    private String spec;
    private PrestoCasterConfig coordinator;
    private PrestoCasterConfig resource;
    private PrestoCasterConfig worker;

    @Alias("trino_name")
    private String trinoName;
    private String image;
}
