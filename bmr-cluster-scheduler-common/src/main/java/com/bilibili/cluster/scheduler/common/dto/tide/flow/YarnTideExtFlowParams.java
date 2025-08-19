package com.bilibili.cluster.scheduler.common.dto.tide.flow;

import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.Data;

@Data
public class YarnTideExtFlowParams extends TideExtFlowParams {

    // 预期Pod数量
    private int expectedCount;

    // 集群类型
    private TideClusterType clusterType;

}
