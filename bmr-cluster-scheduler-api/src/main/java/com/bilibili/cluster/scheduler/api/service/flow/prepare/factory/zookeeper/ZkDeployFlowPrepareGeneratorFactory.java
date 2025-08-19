package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.zookeeper;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.CommonDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:
 * @Date: 2025/7/2 11:54
 * @Author: nizhiqiang
 */
@Slf4j
@Component
public class ZkDeployFlowPrepareGeneratorFactory extends CommonDeployFlowPrepareGenerateFactory {

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        List<String> nodeList = baseFlowExtPropDTO.getNodeList();
        FlowDeployType deployType = flowEntity.getDeployType();

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);

        List<ExecutionNodeEntity> executionNodeList = Collections.EMPTY_LIST;
        switch (deployType) {
            case CAPACITY_EXPANSION:
                executionNodeList = generateCapcityNodeList(flowEntity, nodeList);
                break;
            case RESTART_SERVICE:
            case ITERATION_RELEASE:
                executionNodeList = getNodeEntityList(flowEntity, nodeList, 1, 1);
                break;
            case OFF_LINE_EVICTION:
                executionNodeList = generateOffLineEvictionNodeList(flowEntity, nodeList);
                break;
        }


        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("save flow {} execution node list success.", flowId);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    private List<ExecutionNodeEntity> generateOffLineEvictionNodeList(ExecutionFlowEntity flowEntity, List<String> nodeList) {

        List<ExecutionNodeEntity> executionNodeList = new LinkedList<>();

        Long flowId = flowEntity.getId();
        final ExecutionNodeEntity firstStageNode = new ExecutionNodeEntity();
        firstStageNode.setExecStage("1");
        firstStageNode.setFlowId(flowId);
        firstStageNode.setNodeName("缩容阶段开始");
        firstStageNode.setOperator(flowEntity.getOperator());
        firstStageNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        firstStageNode.setOperationResult(NodeOperationResult.NORMAL);
        firstStageNode.setBatchId(1);
        firstStageNode.setNodeType(NodeType.STAGE_START_NODE);
        executionNodeList.add(firstStageNode);

        //        第一阶段是缩容节点列表
        List<ExecutionNodeEntity> evictionNodeList = getNodeEntityList(flowEntity, nodeList, 2, nodeList.size());
        evictionNodeList.forEach(node -> node.setExecStage("1"));
        executionNodeList.addAll(evictionNodeList);


        Long componentId = flowEntity.getComponentId();
        Long clusterId = flowEntity.getClusterId();
        List<ComponentNodeDetail> componentZkNodeList = bmrResourceService.queryComponentNodeList(clusterId, componentId);
//        过滤掉当前缩容的节点列表
        componentZkNodeList = componentZkNodeList
                .stream()
                .filter(zkNode -> !nodeList.contains(zkNode.getHostName()))
                .collect(Collectors.toList());

        // 分离hastate为leader的节点
        List<ComponentNodeDetail> leaderNodes = componentZkNodeList
                .stream()
                .filter(node -> "leader".equals(node.getHaState()))
                .collect(Collectors.toList());

        // 移除leader节点
        componentZkNodeList.removeIf(node -> "leader".equals(node.getHaState()));

        // 将leader节点追加到列表末尾
        componentZkNodeList.addAll(leaderNodes);


        if (CollectionUtils.isEmpty(componentZkNodeList)) {
            return executionNodeList;
        }

//        第一阶段最后的批次id
        Integer batchId = executionNodeList.get(executionNodeList.size() - 1).getBatchId();

        final ExecutionNodeEntity secondStageNode = new ExecutionNodeEntity();
        secondStageNode.setExecStage("2");
        secondStageNode.setFlowId(flowId);
        secondStageNode.setNodeName("重启阶段开始");
        secondStageNode.setOperator(flowEntity.getOperator());
        secondStageNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        secondStageNode.setOperationResult(NodeOperationResult.NORMAL);
        secondStageNode.setBatchId(++batchId);
        secondStageNode.setNodeType(NodeType.STAGE_END_NODE);
        executionNodeList.add(secondStageNode);

//        其他扩容节点的重启列表
        for (ComponentNodeDetail componentNodeDetail : componentZkNodeList) {
            ExecutionNodeEntity node = new ExecutionNodeEntity();
            node.setNodeName(componentNodeDetail.getHostName());
            node.setOperator(flowEntity.getOperator());
            node.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            node.setNodeType(NodeType.EXTRA_NODE);
            node.setBatchId(++batchId);
            node.setFlowId(flowEntity.getId());
            node.setOperationResult(NodeOperationResult.NORMAL);
            node.setRack(componentNodeDetail.getRack());
            node.setIp(componentNodeDetail.getIp());
            node.setExecStage("2");
            executionNodeList.add(node);
        }

        final ExecutionNodeEntity clusterCheckNode = new ExecutionNodeEntity();
        clusterCheckNode.setExecStage("3");
        clusterCheckNode.setFlowId(flowId);
        clusterCheckNode.setNodeName("集群状态检测阶段开始");
        clusterCheckNode.setOperator(flowEntity.getOperator());
        clusterCheckNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        clusterCheckNode.setOperationResult(NodeOperationResult.NORMAL);
        clusterCheckNode.setBatchId(++batchId);
        clusterCheckNode.setNodeType(NodeType.STAGE_START_NODE);
        executionNodeList.add(clusterCheckNode);

        return executionNodeList;
    }

    private List<ExecutionNodeEntity> generateCapcityNodeList(ExecutionFlowEntity flowEntity, List<String> nodeList) {

        List<ExecutionNodeEntity> executionNodeList = new LinkedList<>();

        Long flowId = flowEntity.getId();
        final ExecutionNodeEntity firstStageNode = new ExecutionNodeEntity();
        firstStageNode.setExecStage("1");
        firstStageNode.setFlowId(flowId);
        firstStageNode.setNodeName("扩容阶段开始");
        firstStageNode.setOperator(flowEntity.getOperator());
        firstStageNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        firstStageNode.setOperationResult(NodeOperationResult.NORMAL);
        firstStageNode.setBatchId(1);
        firstStageNode.setNodeType(NodeType.STAGE_START_NODE);
        executionNodeList.add(firstStageNode);


//        第一阶段是扩容节点列表
        List<ExecutionNodeEntity> capcityNodeList = getNodeEntityList(flowEntity, nodeList, 2, nodeList.size());
        capcityNodeList.forEach(node -> node.setExecStage("1"));
        executionNodeList.addAll(capcityNodeList);

        Long componentId = flowEntity.getComponentId();
        Long clusterId = flowEntity.getClusterId();

        List<ComponentNodeDetail> componentZkNodeList = bmrResourceService.queryComponentNodeList(clusterId, componentId);
        //        过滤掉当前扩容的节点列表
        componentZkNodeList = componentZkNodeList
                .stream()
                .filter(zkNode -> !nodeList.contains(zkNode.getHostName()))
                .collect(Collectors.toList());

        // 分离hastate为leader的节点
        List<ComponentNodeDetail> leaderNodes = componentZkNodeList
                .stream()
                .filter(node -> "leader".equals(node.getHaState()))
                .collect(Collectors.toList());

        // 移除leader节点
        componentZkNodeList.removeIf(node -> "leader".equals(node.getHaState()));

        // 将leader节点追加到列表末尾
        componentZkNodeList.addAll(leaderNodes);

        Integer batchId = executionNodeList.get(executionNodeList.size() - 1).getBatchId();
        if (!CollectionUtils.isEmpty(componentZkNodeList)) {
            //        第一阶段最后的批次id

            final ExecutionNodeEntity secondStageNode = new ExecutionNodeEntity();
            secondStageNode.setExecStage("2");
            secondStageNode.setFlowId(flowId);
            secondStageNode.setNodeName("重启阶段开始");
            secondStageNode.setOperator(flowEntity.getOperator());
            secondStageNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            secondStageNode.setOperationResult(NodeOperationResult.NORMAL);
            secondStageNode.setBatchId(++batchId);
            secondStageNode.setNodeType(NodeType.STAGE_START_NODE);
            executionNodeList.add(secondStageNode);

//        其他扩容节点的重启列表
            for (ComponentNodeDetail componentNodeDetail : componentZkNodeList) {
                ExecutionNodeEntity node = new ExecutionNodeEntity();
                node.setNodeName(componentNodeDetail.getHostName());
                node.setOperator(flowEntity.getOperator());
                node.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                node.setBatchId(++batchId);
                node.setFlowId(flowEntity.getId());
                node.setNodeType(NodeType.EXTRA_NODE);
                node.setOperationResult(NodeOperationResult.NORMAL);
                node.setRack(componentNodeDetail.getRack());
                node.setIp(componentNodeDetail.getIp());
                node.setExecStage("2");
                executionNodeList.add(node);
            }
        }

        final ExecutionNodeEntity clusterCheckNode = new ExecutionNodeEntity();
        clusterCheckNode.setExecStage("3");
        clusterCheckNode.setFlowId(flowId);
        clusterCheckNode.setNodeName("集群状态检测阶段开始");
        clusterCheckNode.setOperator(flowEntity.getOperator());
        clusterCheckNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        clusterCheckNode.setOperationResult(NodeOperationResult.NORMAL);
        clusterCheckNode.setBatchId(++batchId);
        clusterCheckNode.setNodeType(NodeType.STAGE_START_NODE);
        executionNodeList.add(clusterCheckNode);

        return executionNodeList;
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.ZK_DEPLOY_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
