package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.*;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class PrestoTideDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    BmrResourceV2Service bmrResourceV2Service;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionNodeEventService executionNodeEventService;

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionLogService executionLogService;

    @Resource
    ExecutionFlowService flowService;

    @Resource
    GlobalService globalService;

    // presto 逻辑节点
    private static final String LOGICAL_PRESTO_NODE_NAME = "presto-logical-node";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        final String flowExtParams = baseFlowExtPropDTO.getFlowExtParams();
        PrestoTideExtFlowParams prestoTideExtFlowParams = JSONUtil.toBean(flowExtParams, PrestoTideExtFlowParams.class);

        final long yarnClusterId = prestoTideExtFlowParams.getYarnClusterId();
        final List<MetadataComponentData> metadataComponentDataList = globalService.getBmrMetadataService().queryComponentListByClusterId(yarnClusterId);
        MetadataComponentData targetNodeManagerMetaData = null;
        for (MetadataComponentData metadataComponentData : metadataComponentDataList) {
            if (metadataComponentData.getComponentName().equalsIgnoreCase("NodeManager")) {
                targetNodeManagerMetaData = metadataComponentData;
                break;
            }
        }
        Preconditions.checkNotNull(targetNodeManagerMetaData, "metadata of NodeManager not fond");
        prestoTideExtFlowParams.setComponentId(targetNodeManagerMetaData.getId());
        // back fill componentId
        baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(prestoTideExtFlowParams));
        executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);

        List<ExecutionNodeEntity> executionNodeList;
        final FlowDeployType deployType = flowEntity.getDeployType();
        switch (deployType) {
            case PRESTO_TIDE_ON:
                executionNodeList = getPrestoTideOnNodeList(flowEntity,
                        prestoTideExtFlowParams.getAppId(), TideNodeStatus.STAIN);
                int yarnNodeCnt = executionNodeList.size() - 1;
                int tolerance = ((Double) (yarnNodeCnt * 0.2)).intValue();
                if (tolerance > 0) {
                    flowService.updateFlowTolerance(flowId, tolerance);
                }
                break;
            case PRESTO_TIDE_OFF:
                executionNodeList = getPrestoTideOffNodeList(flowEntity);
                break;
            default:
                throw new IllegalArgumentException("un-support deploy type of: " + deployType);
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(executionNodeService.batchInsert(split), "批量插入execution node失败");
        }

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("PrestoTideDeployFlowPrepareGenerateFactory#resolvePipelineEventList is {}", resolvedEventList);

        executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    private List<ExecutionNodeEntity> getPrestoTideOnNodeList(ExecutionFlowEntity flowEntity,
                                                              String appId, TideNodeStatus tideNodeStatus) {
        // stage-1 yarn下线节点
        List<TideNodeDetail> waitOffYarnNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, tideNodeStatus, TideClusterType.PRESTO);
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

        // stage-2 presto 扩容
        final ExecutionNodeEntity prestoExpansionLogicalNode = new ExecutionNodeEntity();
        prestoExpansionLogicalNode.setFlowId(flowEntity.getId());
        prestoExpansionLogicalNode.setNodeName(LOGICAL_PRESTO_NODE_NAME);
        prestoExpansionLogicalNode.setBatchId(2);
        prestoExpansionLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        prestoExpansionLogicalNode.setExecStage("2");
        prestoExpansionLogicalNode.setOperator(flowEntity.getOperator());
        prestoExpansionLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        executionNodeList.add(prestoExpansionLogicalNode);
        return executionNodeList;
    }

    private List<ExecutionNodeEntity> getPrestoTideOffNodeList(ExecutionFlowEntity flowEntity) {
        // presto缩容完成需动态生生成yarn扩容即节点
        final ExecutionNodeEntity prestoShrinkLogicalNode = new ExecutionNodeEntity();
        prestoShrinkLogicalNode.setFlowId(flowEntity.getId());
        prestoShrinkLogicalNode.setNodeName(LOGICAL_PRESTO_NODE_NAME);
        prestoShrinkLogicalNode.setBatchId(1);
        prestoShrinkLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        prestoShrinkLogicalNode.setExecStage("1");
        prestoShrinkLogicalNode.setOperator(flowEntity.getOperator());
        prestoShrinkLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        return Arrays.asList(prestoShrinkLogicalNode);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.PRESTO_TIDE_ON, FlowDeployType.PRESTO_TIDE_OFF);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.PRESTO_TIDE_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
