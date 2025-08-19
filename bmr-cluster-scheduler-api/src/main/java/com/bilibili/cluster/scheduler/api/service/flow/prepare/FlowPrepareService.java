package com.bilibili.cluster.scheduler.api.service.flow.prepare;

import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;

import javax.annotation.Nullable;
import java.util.List;

public interface FlowPrepareService {

    void prepareFlowExecuteNodeAndEvents(ExecutionFlowEntity flowEntity);

    List<ResolvedEvent> resolvePipelineEventList(@Nullable DeployOneFlowReq req,
                                                 ExecutionFlowEntity flowEntity,
                                                 @Nullable MetadataClusterData clusterDetail,
                                                 @Nullable MetadataComponentData componentDetail) throws Exception;

}
