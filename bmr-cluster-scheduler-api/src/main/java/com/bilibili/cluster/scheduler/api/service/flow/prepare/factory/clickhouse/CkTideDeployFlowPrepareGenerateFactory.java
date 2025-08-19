package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.clickhouse;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.*;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.metric.MetricService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
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
public class CkTideDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

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
    MetricService metricService;

    @Resource
    ExecutionFlowService flowService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    GlobalService globalService;

    // presto 逻辑节点
    private static final String LOGICAL_PRESTO_NODE_NAME = "ck-logical-node";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        final String flowExtParams = baseFlowExtPropDTO.getFlowExtParams();
        CkTideExtFlowParams ckTideExtFlowParams = JSONUtil.toBean(flowExtParams, CkTideExtFlowParams.class);

        final long yarnClusterId = ckTideExtFlowParams.getYarnClusterId();
        final List<MetadataComponentData> metadataComponentDataList = globalService.getBmrMetadataService().queryComponentListByClusterId(yarnClusterId);
        MetadataComponentData targetNodeManagerMetaData = null;
        for (MetadataComponentData metadataComponentData : metadataComponentDataList) {
            if (metadataComponentData.getComponentName().equalsIgnoreCase("NodeManager")) {
                targetNodeManagerMetaData = metadataComponentData;
                break;
            }
        }
        Preconditions.checkNotNull(targetNodeManagerMetaData, "metadata of NodeManager not fond");
        ckTideExtFlowParams.setNodeManagerComponentId(targetNodeManagerMetaData.getId());

        Long componentId = flowEntity.getComponentId();
        //        默认潮汐使用最新的配置的版本
        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailByComponentId(componentId);
        Long configVersionId = configDetailData.getId();
        Preconditions.checkNotNull(targetNodeManagerMetaData, String.format("can not find last config id %s, component id is %s", configVersionId, componentId));
        ckTideExtFlowParams.setConfigId(configVersionId);

        // back fill componentId
        baseFlowExtPropDTO.setFlowExtParams(JSONUtil.toJsonStr(ckTideExtFlowParams));
        executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);

        List<ExecutionNodeEntity> executionNodeList;
        final FlowDeployType deployType = flowEntity.getDeployType();
        switch (deployType) {
            case CK_TIDE_ON:
                executionNodeList = getCkTideOnNodeList(flowEntity,
                        ckTideExtFlowParams.getAppId());
                int yarnNodeCnt = executionNodeList.size() - 1;
//                todo:目前节点太少设置容错度最少为1
                int tolerance = Math.max(((Double) (yarnNodeCnt * 0.2)).intValue(), 1);
                if (tolerance > 0) {
                    flowService.updateFlowTolerance(flowId, tolerance);
                }
                break;
            case CK_TIDE_OFF:
                executionNodeList = getCkTideOffNodeList(flowEntity);
                break;
            default:
                throw new IllegalArgumentException("un-support deploy type of: " + deployType);
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(executionNodeService.batchInsert(split), "批量插入execution node失败");
        }

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("CkTideDeployFlowPrepareGenerateFactory#resolvePipelineEventList is {}", resolvedEventList);

        executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    private List<ExecutionNodeEntity> getCkTideOnNodeList(ExecutionFlowEntity flowEntity,
                                                          String appId) {
        // stage-1 yarn下线节点
        List<TideNodeDetail> waitOffYarnNodeList = globalService.getBmrResourceV2Service().queryTideOnBizUsedNodes(appId, TideNodeStatus.STAIN, TideClusterType.CLICKHOUSE);
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

        // stage-2 ck 扩容
        final ExecutionNodeEntity ckExpansionLogicalNode = new ExecutionNodeEntity();
        ckExpansionLogicalNode.setFlowId(flowEntity.getId());
        ckExpansionLogicalNode.setNodeName(LOGICAL_PRESTO_NODE_NAME);
        ckExpansionLogicalNode.setBatchId(2);
        ckExpansionLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        ckExpansionLogicalNode.setExecStage("2");
        ckExpansionLogicalNode.setOperator(flowEntity.getOperator());
        ckExpansionLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        executionNodeList.add(ckExpansionLogicalNode);
        return executionNodeList;
    }

    private List<ExecutionNodeEntity> getCkTideOffNodeList(ExecutionFlowEntity flowEntity) {
        // presto缩容完成需动态生生成yarn扩容即节点
        final ExecutionNodeEntity ckShrinkLogicalNode = new ExecutionNodeEntity();
        ckShrinkLogicalNode.setFlowId(flowEntity.getId());
        ckShrinkLogicalNode.setNodeName(LOGICAL_PRESTO_NODE_NAME);
        ckShrinkLogicalNode.setBatchId(1);
        ckShrinkLogicalNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        ckShrinkLogicalNode.setExecStage("1");
        ckShrinkLogicalNode.setOperator(flowEntity.getOperator());
        ckShrinkLogicalNode.setOperationResult(NodeOperationResult.NORMAL);

        return Arrays.asList(ckShrinkLogicalNode);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.CK_TIDE_ON, FlowDeployType.CK_TIDE_OFF);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.CK_TIDE_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
