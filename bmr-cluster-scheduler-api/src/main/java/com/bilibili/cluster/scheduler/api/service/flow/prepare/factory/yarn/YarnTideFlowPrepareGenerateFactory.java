package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.yarn;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.tide.flow.YarnTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
public class YarnTideFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionNodeEventService executionNodeEventService;

    @Resource
    GlobalService globalService;

    private static final String LOGICAL_NODE_NAME = "tide-expansion-prepare-logical-node";


    @Override
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        final YarnTideExtFlowParams tideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, YarnTideExtFlowParams.class);
        final TideClusterType clusterType = tideExtFlowParams.getClusterType();
        Preconditions.checkState(TideClusterType.PRESTO.equals(clusterType), clusterType + " now is not supported");

        final FlowDeployType deployType = flowEntity.getDeployType();

        List<ExecutionNodeEntity> executionNodeList;
        switch (deployType) {
            case YARN_TIDE_EXPANSION:
                executionNodeList = getStage1PrepareNodeList(flowEntity);
                break;
            case YARN_TIDE_SHRINK:
                executionNodeList = getRequireOfflineNodeList(flowEntity,
                        tideExtFlowParams.getAppId(), TideNodeStatus.STAIN, clusterType);
                int yarnNodeCnt = executionNodeList.size() - 1;
                int tolerance = ((Double) (yarnNodeCnt * 0.2)).intValue();
                if (tolerance > 0) {
                    flowService.updateFlowTolerance(flowId, tolerance);
                }
                break;
            default:
                throw new IllegalArgumentException("un-support deploy type of: " + deployType);
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(executionNodeService.batchInsert(split), "批量插入execution node失败");
        }

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("YarnTideFlowPrepareGenerateFactory#resolvePipelineEventList is {}", resolvedEventList);

        executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    private List<ExecutionNodeEntity> getStage1PrepareNodeList(ExecutionFlowEntity flowEntity) {
        // 等待节点可用, 动态生生成二阶段yarn扩容即节点
        final ExecutionNodeEntity prestoShrinkLogicalNode = new ExecutionNodeEntity();
        prestoShrinkLogicalNode.setFlowId(flowEntity.getId());
        prestoShrinkLogicalNode.setNodeName(LOGICAL_NODE_NAME);
        prestoShrinkLogicalNode.setBatchId(1);
        prestoShrinkLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        prestoShrinkLogicalNode.setExecStage("1");
        prestoShrinkLogicalNode.setOperator(flowEntity.getOperator());
        prestoShrinkLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        return Arrays.asList(prestoShrinkLogicalNode);
    }

    private List<ExecutionNodeEntity> getRequireOfflineNodeList(ExecutionFlowEntity flowEntity,
                 String appId, TideNodeStatus tideNodeStatus, TideClusterType clusterType) {
        // stage-1 yarn下线节点
        List<TideNodeDetail> waitOffYarnNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, tideNodeStatus, clusterType);
        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        for (TideNodeDetail nodeDetail : waitOffYarnNodeList) {
            String hostname = nodeDetail.getHostName();
            final ExecutionNodeEntity nodeEntity = new ExecutionNodeEntity();
            nodeEntity.setNodeName(hostname);
            nodeEntity.setBatchId(1);
            nodeEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            nodeEntity.setFlowId(flowEntity.getId());
            nodeEntity.setExecStage("1");
            nodeEntity.setOperator(flowEntity.getOperator());
            nodeEntity.setOperationResult(NodeOperationResult.NORMAL);
            nodeEntity.setIp(nodeDetail.getIp());
            executionNodeList.add(nodeEntity);
        }
        return executionNodeList;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.YARN_TIDE_EXPANSION, FlowDeployType.YARN_TIDE_SHRINK);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.YARN_TIDE_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
