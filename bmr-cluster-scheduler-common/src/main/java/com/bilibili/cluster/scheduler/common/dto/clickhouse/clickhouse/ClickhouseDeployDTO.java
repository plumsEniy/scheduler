package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse;

import cn.hutool.core.annotation.Alias;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ChConfig;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ChTemplate;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

/**
 * @description:
 * @Date: 2025/2/7 16:46
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class ClickhouseDeployDTO {

    /**
     * 是否预览
     * 如果true预览则会返回最终生成的模版而不生效
     * 如果false则发布生效
     */
    private boolean preview;

    /**
     * 这里的集群名不是配置文件中的集群名是写死的caster的集群名
     */
    @Alias("cluster_name")
    private String clusterName;

    private String env;

    @Alias("app_id")
    private String appId;

    @Alias("resource_pool_name")
    private String resourcePoolName;

    @Alias("ch_template")
    private ChTemplate chTemplate;

    @Alias("ch_config")
    private ChConfig chConfig;

    /**
     * 空的
     */
    @Alias("env_list")
    private List envList = new LinkedList();
}
