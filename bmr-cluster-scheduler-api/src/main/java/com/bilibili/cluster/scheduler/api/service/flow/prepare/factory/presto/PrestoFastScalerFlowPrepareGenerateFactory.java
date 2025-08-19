package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
public class PrestoFastScalerFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionNodeEventService executionNodeEventService;


    private static final String LOGICAL_PRESTO_NODE_NAME = "presto-logical-node";

    @Override
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        final PrestoFastScalerExtFlowParams extFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);

        final FlowDeployType deployType = flowEntity.getDeployType();
        List<ExecutionNodeEntity> executionNodeList;
        switch (deployType) {
            case PRESTO_FAST_EXPANSION:
            case PRESTO_FAST_SHRINK:
                executionNodeList = generatePrestoLogicalNode(flowEntity);
                break;
            default:
                throw new IllegalArgumentException("un-support deploy type of: " + deployType);
        }
        flowService.updateFlowTolerance(flowId, 0);
        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(executionNodeService.batchInsert(split), "批量插入execution node失败");
        }

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("PrestoFastScalerFlowPrepareGenerateFactory#resolvePipelineEventList is {}", resolvedEventList);

        executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    private List<ExecutionNodeEntity> generatePrestoLogicalNode(ExecutionFlowEntity flowEntity) {
        final ExecutionNodeEntity prestoLogicalNode = new ExecutionNodeEntity();
        prestoLogicalNode.setFlowId(flowEntity.getId());
        prestoLogicalNode.setNodeName(LOGICAL_PRESTO_NODE_NAME);
        prestoLogicalNode.setBatchId(1);
        prestoLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        prestoLogicalNode.setExecStage("1");
        prestoLogicalNode.setOperator(flowEntity.getOperator());
        prestoLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        return Arrays.asList(prestoLogicalNode);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.PRESTO_FAST_EXPANSION, FlowDeployType.PRESTO_FAST_SHRINK);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.PRESTO_FAST_SCALER_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
