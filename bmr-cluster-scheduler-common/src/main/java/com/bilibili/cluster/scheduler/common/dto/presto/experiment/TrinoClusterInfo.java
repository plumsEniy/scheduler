package com.bilibili.cluster.scheduler.common.dto.presto.experiment;

import lombok.Data;

@Data
public class TrinoClusterInfo {

    /**
     * 是否重建trino集群
     */
    private boolean rebuildCluster;

    /**
     * 集群Id
     */
    private long clusterId;

    /**
     * 组件Id
     */
    private long componentId;

    /**
     * 配置Id
     */
    private long configId;

    /**
     * 安装包Id
     */
    private long packId;

}
