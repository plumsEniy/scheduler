package com.bilibili.cluster.scheduler.api.event.analyzer;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineParameter {

    // apply req
    private DeployOneFlowReq req;

    // flow base info
    private ExecutionFlowEntity flowEntity;

    // cluster info
    private MetadataClusterData clusterData;

    // component info
    private MetadataComponentData componentData;

}
