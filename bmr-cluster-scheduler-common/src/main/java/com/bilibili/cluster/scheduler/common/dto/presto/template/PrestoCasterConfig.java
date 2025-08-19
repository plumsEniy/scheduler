package com.bilibili.cluster.scheduler.common.dto.presto.template;

import lombok.Data;

/**
 * @description:
 * @Date: 2024/7/10 17:56
 * @Author: nizhiqiang
 */

@Data
public class PrestoCasterConfig {
    Integer cpu;
    Integer mem;
    Integer count;
}
