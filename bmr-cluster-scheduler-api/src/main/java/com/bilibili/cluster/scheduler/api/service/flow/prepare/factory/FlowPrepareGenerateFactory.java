package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;

import javax.annotation.Nullable;
import java.util.List;

public interface FlowPrepareGenerateFactory {

    void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception;


    List<FlowDeployType> fitDeployType();

    List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception;

    default boolean isDefault() {
        return false;
    }

    default List<ResolvedEvent> resolvePipelineEventList(@Nullable DeployOneFlowReq req,
                                                         ExecutionFlowEntity flowEntity,
                                                         @Nullable MetadataClusterData clusterDetail,
                                                         @Nullable MetadataComponentData componentDetail) throws Exception {
        final PipelineParameter pipelineParameter = new PipelineParameter(req, flowEntity, clusterDetail, componentDetail);
        return resolvePipelineEventList(pipelineParameter);
    }

    default String getName() {
        return getClass().getSimpleName();
    }

}
