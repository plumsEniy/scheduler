package com.bilibili.cluster.scheduler.common.dto.presto.iteration;

import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import lombok.Data;

@Data
public class PrestoIterationExtNodeParams extends BaseNodeParams {

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
