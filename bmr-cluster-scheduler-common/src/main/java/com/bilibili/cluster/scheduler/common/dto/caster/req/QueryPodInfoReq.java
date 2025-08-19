package com.bilibili.cluster.scheduler.common.dto.caster.req;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description: 查询pod信息
 * @Date: 2024/7/22 11:06
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
public class QueryPodInfoReq {

    @Alias("cluster_id")
    private long clusterId;
    private String podselector;

    private String hostnames;
    private Boolean withPvcInfo = true;
    private String namespace;
}
