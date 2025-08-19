package com.bilibili.cluster.scheduler.common.dto.presto.template;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 部署presto
 * @Date: 2024/7/10 15:26
 * @Author: nizhiqiang
 */

@NoArgsConstructor
@Data
public class PrestoDeployDTO {

    @Alias("app_id")
    private String appId;
    private String env;
    @Alias("cluster_name")
    private String clusterName;


    @Alias("resource_pool_name")
    private String resourcePoolName;

    private PrestoCasterTemplate template;

    private boolean preview;

}
