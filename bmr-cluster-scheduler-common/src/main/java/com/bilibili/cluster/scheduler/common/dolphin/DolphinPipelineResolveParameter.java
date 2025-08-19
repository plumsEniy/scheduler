package com.bilibili.cluster.scheduler.common.dolphin;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import lombok.Data;

@Data
public class DolphinPipelineResolveParameter {

    private String roleName;

    private String clusterName;

    /**
     * 集群别名
     */
    private String clusterAlias;

    private String componentName;

    private FlowDeployType flowDeployType;

    /**
     * pipeline group index, default 0, means only has one pipeline definition
     */
    private int chainedIndex;

}
