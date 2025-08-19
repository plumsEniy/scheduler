package com.bilibili.cluster.scheduler.api.service.flow.rollback.phase.nnproxy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.NNProxyDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.phase.AbstractNewPhasedFlowRollbackFactory;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployRollbackNodeExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.*;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NNProxyDeployRollbackFactory extends AbstractNewPhasedFlowRollbackFactory {

    @Resource
    NNProxyDeployFlowPrepareGenerateFactory deployFlowPrepareGenerateFactory;

    @Resource
    BmrResourceService bmrResourceService;

    @Override
    public boolean supportRollback(ExecutionFlowEntity flowEntity) {
        long flowId = flowEntity.getId();
        NNProxyDeployFlowExtParams flowExtParams = flowPropsService.getFlowExtParamsByCache(flowId, NNProxyDeployFlowExtParams.class);
        final SubDeployType subDeployType = flowExtParams.getSubDeployType();
        if (subDeployType == SubDeployType.ITERATION_RELEASE) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean doRollback(FlowOperateButtonEnum buttonType, ExecutionFlowEntity flowEntity) throws Exception {
        Preconditions.checkState(FlowOperateButtonEnum.FULL_ROLLBACK == buttonType,
                getName() + ": doRollback require 'FULL_ROLLBACK'");
        long flowId = flowEntity.getId();
        ExecutionFlowEntity executionFlow = flowService.getById(flowId);
        Preconditions.checkState(FlowStatusEnum.IN_ROLLBACK.equals(executionFlow.getFlowStatus()),
                "工作流状态需要为: [IN_ROLLBACK] 状态，当前为: " + executionFlow.getFlowStatus());

        // 已经处于全局回滚状态
        final FlowRollbackType flowRollbackType = executionFlow.getFlowRollbackType();
        if (flowRollbackType == FlowRollbackType.GLOBAL || flowRollbackType == FlowRollbackType.PREPARE_GLOBAL) {
            return true;
        }

        flowService.updateFlowRollbackType(FlowRollbackType.PREPARE_GLOBAL, flowId);

        final Integer curBatchId = executionFlow.getCurBatchId();
        final LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0)
                .le(ExecutionNodeEntity::getBatchId, curBatchId)
                .eq(ExecutionNodeEntity::getExecType, NodeExecType.FORWARD)
                .set(ExecutionNodeEntity::getExecType, NodeExecType.WAITING_ROLLBACK);
        nodeService.update(updateWrapper);

        prepareGlobalRollback(flowEntity);

        return true;
    }

    private void fastFinishRollback(ExecutionFlowEntity flowEntity) {
        final Long flowId = flowEntity.getId();
        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setFlowId(flowId);
        updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.ROLLBACK_SUCCESS);
        updateExecutionFlowDTO.setRollbackType(FlowRollbackType.GLOBAL);
        flowService.updateFlow(updateExecutionFlowDTO);

        bmrFlowService.alterFlowStatus(flowId, FlowOperateButtonEnum.TERMINATE);
    }

    public void prepareGlobalRollback(ExecutionFlowEntity flowEntity) throws Exception {
        long flowId = flowEntity.getId();
        final Integer curBatchId = flowEntity.getCurBatchId();
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .le(ExecutionNodeEntity::getBatchId, curBatchId);
        List<ExecutionNodeEntity> nodeEntityList = nodeService.list(queryWrapper);

        if (CollectionUtils.isEmpty(nodeEntityList)) {
            fastFinishRollback(flowEntity);
            return;
        }

        // 组件内根据DNS对节点分组
        final Map<Long, Map<String, List<ExecutionNodeEntity>>> dnsMap = new TreeMap<>();
        Map<String, NNProxyDeployNodeExtParams> hostToPropsMap = new HashMap<>();
        for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
            final NodeType nodeType = nodeEntity.getNodeType();
            if (nodeType != NodeType.NORMAL) {
                continue;
            }

            final Long nodeId = nodeEntity.getId();
            final NNProxyDeployNodeExtParams nodeExtParams = nodePropsService.queryNodePropsByNodeId(nodeId, NNProxyDeployNodeExtParams.class);
            final String dnsHost = nodeExtParams.getDnsHost();
            if (StringUtils.isBlank(dnsHost)) {
                continue;
            }
            String hostname = nodeEntity.getNodeName();
            dnsMap.computeIfAbsent(nodeExtParams.getComponentId(), k -> new TreeMap<>())
                    .computeIfAbsent(dnsHost, v -> new ArrayList<>()).add(nodeEntity);
            hostToPropsMap.put(hostname, nodeExtParams);
        }

        if (MapUtils.isEmpty(dnsMap)) {
            fastFinishRollback(flowEntity);
            return;
        }

        // 对分组节点排序，按节点数量由大到小排序
//        List<Map.Entry<String, List<ExecutionNodeEntity>>> collect = dnsToReExecNodes.entrySet()
//                .stream()
//                .sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder()))
//                .collect(Collectors.toList());

        Map<Integer, List<ExecutionNodeEntity>> batchToHostList = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<String, List<ExecutionNodeEntity>>> componentEntry : dnsMap.entrySet()) {
            Map<String, List<ExecutionNodeEntity>> dnsToHostList = componentEntry.getValue();
            int index = 0;
            for (Map.Entry<String, List<ExecutionNodeEntity>> dnsEntry : dnsToHostList.entrySet()) {
                index++;
                batchToHostList.computeIfAbsent(index, i -> new ArrayList<>()).addAll(dnsEntry.getValue());
            }
        }

        // 查询获取当前最大stage和最大batchId
        final String maxStage = nodeService.queryMaxStageByFlowId(flowId);
        int curMaxStage;
        // 紧急发布场景不存在stage切分
        if (StringUtils.isBlank(maxStage)) {
            curMaxStage = 1;
        } else {
            curMaxStage = Integer.parseInt(maxStage);
        }
        int curMaxBatchId = nodeService.queryMaxBatchId(flowId);

        int nextStage = curMaxStage + 1;
        String nextStageValue = String.valueOf(nextStage);
        // 获取待回滚的节点列表
        List<ExecutionNodeEntity> nextStageNodeEntityList = new ArrayList<>();
        int nextBatchId = curMaxBatchId + 1;

        // 填充stage开始节点
        final ExecutionNodeEntity stageStartNode = new ExecutionNodeEntity();
        stageStartNode.setExecStage(nextStageValue);
        stageStartNode.setFlowId(flowId);
        stageStartNode.setNodeName("回滚" + nextStageValue + "阶段开始");
        stageStartNode.setOperator(flowEntity.getOperator());
        stageStartNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        stageStartNode.setOperationResult(NodeOperationResult.NORMAL);
        stageStartNode.setBatchId(nextBatchId);
        stageStartNode.setNodeType(NodeType.STAGE_START_NODE);
        nextStageNodeEntityList.add(stageStartNode);

        // 填充待回滚的业务开始
        for (Map.Entry<Integer, List<ExecutionNodeEntity>> entry : batchToHostList.entrySet()) {
            nextBatchId++;
            final List<ExecutionNodeEntity> preNodeList = entry.getValue();

            for (ExecutionNodeEntity node : preNodeList) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(nextStageValue);
                jobEntity.setFlowId(flowId);
                jobEntity.setNodeName(node.getNodeName());
                jobEntity.setOperator(flowEntity.getOperator());

                final NodeExecuteStatusEnum nodeStatus = node.getNodeStatus();
                if (nodeStatus.canRollBack()) {
                    jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
                } else {
                    jobEntity.setNodeStatus(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
                }
                jobEntity.setBatchId(nextBatchId);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                nextStageNodeEntityList.add(jobEntity);
            }
        }

        // 填充回滚的结束节点
        final ExecutionNodeEntity stageEndNode = new ExecutionNodeEntity();
        stageEndNode.setExecStage(nextStageValue);
        stageEndNode.setFlowId(flowId);
        stageEndNode.setNodeName("回滚" + nextStageValue + "阶段结束");
        stageEndNode.setOperator(flowEntity.getOperator());
        stageEndNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        stageEndNode.setBatchId(++nextBatchId);
        stageEndNode.setOperationResult(NodeOperationResult.NORMAL);
        stageEndNode.setNodeType(NodeType.STAGE_END_NODE);
        nextStageNodeEntityList.add(stageEndNode);

        nodeService.batchInsert(nextStageNodeEntityList);
        // 更新当前批次节点状态至【回滚状态】
        final LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0)
                .eq(ExecutionNodeEntity::getExecStage, nextStageValue)
                .eq(ExecutionNodeEntity::getExecType, NodeExecType.FORWARD)
                .set(ExecutionNodeEntity::getExecType, NodeExecType.ROLLBACK);
        nodeService.update(updateWrapper);

        log.info("{}: rollback stage node generate ok, flowId={}", getName(), flowId);

        for (ExecutionNodeEntity nodeEntity : nextStageNodeEntityList) {
            if (!nodeEntity.getNodeType().isNormalExecNode()) {
                continue;
            }
            String hostname = nodeEntity.getNodeName();
            NNProxyDeployNodeExtParams nodeExtParams = hostToPropsMap.get(hostname);
            final Long nodeId = nodeEntity.getId();
            NNProxyDeployRollbackNodeExtParams copyNodeProps = new NNProxyDeployRollbackNodeExtParams();
            BeanUtils.copyProperties(nodeExtParams, copyNodeProps);
            copyNodeProps.setNodeId(nodeId);
            copyNodeProps.setPreNodeId(nodeExtParams.getNodeId());
            nodePropsService.saveNodeProp(nodeId, copyNodeProps);
        }

        final PipelineParameter pipelineParameter = new PipelineParameter();
        pipelineParameter.setFlowEntity(flowEntity);
        final List<ResolvedEvent> resolvedEventList = deployFlowPrepareGenerateFactory.resolvePipelineEventList(pipelineParameter);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, nextStageNodeEntityList, resolvedEventList);
        log.info("{}: save rollback {} execution node event success, flowId={}", getName(), flowId);

        int startBatchId = curMaxBatchId + 1;
        flowService.updateCurrentBatchIdByFlowId(flowId, startBatchId);
        log.info("{}: flow set curBatchId to {}, flowId={}", getName(), startBatchId, flowId);
        flowService.updateCurFault(flowId, 0);
        log.info("{}: flow reset curFault to {}, flowId={}", getName(), 0, flowId);

        flowService.updateFlowRollbackType(FlowRollbackType.GLOBAL, flowId);
        log.info("{}: flow rollback type change to {}, flowId={}", getName(), FlowRollbackType.GLOBAL, flowId);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.NNPROXY_DEPLOY);
    }

    @Override
    public void handlerUnRollbackNodes(ExecutionFlowEntity flowEntity) {
        final Long flowId = flowEntity.getId();
        final ExecutionNodeEntity queryDO = new ExecutionNodeEntity();
        queryDO.setFlowId(flowId);
        queryDO.setNodeStatus(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
        queryDO.setNodeType(null);

        final List<ExecutionNodeEntity> nodeEntityList = nodeService.queryNodeList(queryDO, true);
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return;
        }

        String warnMsg = String.format("require handle of 'IN_NODE_EXECUTE' node list size is %s", nodeEntityList.size());
        logService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
        for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
            long nodeId = nodeEntity.getId();
            NNProxyDeployRollbackNodeExtParams nodeExtParams = nodePropsService.queryNodePropsByNodeId(nodeId, NNProxyDeployRollbackNodeExtParams.class);
            long preNodeId = nodeExtParams.getPreNodeId();

            final ExecutionNodeEntity preNodeEntity = nodeService.getById(preNodeId);
            final NodeExecuteStatusEnum preNodeStatus = preNodeEntity.getNodeStatus();

            if (preNodeStatus.canRollBack()) {
                nodeService.batchUpdateNodeForReadyExec(flowId, Arrays.asList(nodeId), NodeExecType.ROLLBACK, NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
                continue;
            }
            // 边缘状态未执行任务切换至回滚跳过状态
            switch (preNodeStatus) {
                case SKIPPED:
                    nodeService.batchUpdateNodeForReadyExec(flowId, Arrays.asList(nodeId), NodeExecType.ROLLBACK, NodeExecuteStatusEnum.ROLLBACK_SKIPPED);
                    break;
                case UN_NODE_EXECUTE:
                case RECOVERY_UN_NODE_EXECUTE:
                    nodeService.batchUpdateNodeForReadyExec(flowId, Arrays.asList(nodeId), NodeExecType.ROLLBACK, NodeExecuteStatusEnum.ROLLBACK_SKIPPED_WHEN_UN_NODE_EXECUTE);
                    break;
            }
        }
    }
}
