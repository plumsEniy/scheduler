package com.bilibili.cluster.scheduler.api.service.flow.prepare.bus;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;

public interface FlowPrepareGenerateFactoryBusService {

    FlowPrepareGenerateFactory forwardFlowPrepareGenerateFactory(PipelineParameter pipelineParameter);

    FlowPrepareGenerateFactory forwardFlowPrepareGenerateFactory(ExecutionFlowEntity flowEntity);
}
