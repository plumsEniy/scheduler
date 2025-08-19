package com.bilibili.cluster.scheduler.api.service.flow.rollback.symmetric;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.FlowRollbackFactory;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 完全对称类型的回滚工厂
 */
@Slf4j
@Component
public class SymmetricFlowRollbackFactory implements FlowRollbackFactory {

    @Resource
    ExecutionFlowService executionFlowService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionLogService executionLogService;

    @Resource
    BmrFlowService bmrFlowService;

    @Override
    public boolean supportRollback(ExecutionFlowEntity flowEntity) {
        return true;
    }

    @Override
    public List<FlowOperateButtonEnum> getRollbackButton(ExecutionFlowEntity flowEntity) {
        return Arrays.asList(FlowOperateButtonEnum.STAGED_ROLLBACK, FlowOperateButtonEnum.FULL_ROLLBACK);
    }

    @Override
    public boolean doRollback(FlowOperateButtonEnum buttonType, ExecutionFlowEntity flowEntity) {
        long flowId = flowEntity.getId();
        switch (buttonType) {
            case STAGED_ROLLBACK:
                handleRollbackWithStaged(flowId);
                break;
            case FULL_ROLLBACK:
                handleRollbackWithFullList(flowId);
                break;
            default:
                throw new IllegalArgumentException("un-support rollback button: " + buttonType);
        }
        return true;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.SPARK_DEPLOY, FlowDeployType.SPARK_PERIPHERY_COMPONENT_DEPLOY);
    }

    @Override
    public void handlerUpdateFlowStatus(ExecutionFlowEntity flowEntity) {
        final Long flowId = flowEntity.getId();
        Integer currentBatchId = flowEntity.getCurBatchId();

        final FlowRollbackType rollbackType = flowEntity.getFlowRollbackType();
        int minExecBatchId = 1;
        if (rollbackType.equals(FlowRollbackType.STAGE)) {
            String curStage = executionNodeService.queryCurStage(flowId, currentBatchId);
            minExecBatchId = executionNodeService.queryMinBatchIdByStage(flowId, curStage);
        }
        // 处理待回滚节点
        final ExecutionNodeEntity queryDO = new ExecutionNodeEntity();
        queryDO.setFlowId(flowId);
        queryDO.setExecType(NodeExecType.WAITING_ROLLBACK);
        queryDO.setNodeType(null);
        final List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeList(queryDO, true);
        if (!CollectionUtils.isEmpty(nodeEntityList)) {
            String warnMsg = String.format("require handle of 'WAITING_ROLLBACK' node list size is %s", nodeEntityList.size());
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
            Map<NodeExecuteStatusEnum, List<ExecutionNodeEntity>> statusNodesMap = new HashMap<>();
            Map<NodeExecuteStatusEnum, List<Long>> statusNodeIdsMap = new HashMap<>();

            for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
                final NodeExecuteStatusEnum nodeStatus = nodeEntity.getNodeStatus();
                statusNodesMap.computeIfAbsent(nodeStatus, K -> new ArrayList<>()).add(nodeEntity);
                statusNodeIdsMap.computeIfAbsent(nodeStatus, K -> new ArrayList<>()).add(nodeEntity.getId());
            }

            for (Map.Entry<NodeExecuteStatusEnum, List<Long>> entry : statusNodeIdsMap.entrySet()) {
                final NodeExecuteStatusEnum nodeStatus = entry.getKey();
                final List<Long> nodeIds = entry.getValue();
                if (nodeStatus.canRollBack()) {
                    executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.ROLLBACK, NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
                    continue;
                }

                // 边缘状态未执行任务切换至回滚跳过状态
                switch (nodeStatus) {
                    case SKIPPED:
                        executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.ROLLBACK, NodeExecuteStatusEnum.ROLLBACK_SKIPPED);
                        break;
                    case UN_NODE_EXECUTE:
                    case RECOVERY_UN_NODE_EXECUTE:
                        executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.ROLLBACK, NodeExecuteStatusEnum.ROLLBACK_SKIPPED_WHEN_UN_NODE_EXECUTE);
                        break;
                }
            }
        }

        List<NodeExecuteStatusEnum> nodeStatus = new ArrayList<>();
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);
        List<ExecutionNodeEntity> executionUnNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
        if (!CollectionUtils.isEmpty(executionUnNodeEntities)) {
            // 存在待执行的, flow状态不做任何变更
            return;
        }

        nodeStatus.clear();
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE);
        List<ExecutionNodeEntity> executionRunningNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
        if (!CollectionUtils.isEmpty(executionRunningNodeEntities)) {
            // 当前批次有任务处于执行中, 更新flow状态为执行中
            // executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.IN_ROLLBACK);

            String logMsg = String.format("current batch id: %s has node status is IN_ROLLBACK", currentBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);
            return;
        }

        // 清空
        nodeStatus.clear();
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE);
        List<ExecutionNodeEntity> executionFailNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);

        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setFlowId(flowId);

        if (!CollectionUtils.isEmpty(executionFailNodeEntities)) {
            final Integer tolerance = flowEntity.getTolerance();
            // 当前批次失败节点数
            int failCount = executionFailNodeEntities.size();
            int totalFailCount = flowEntity.getCurFault() + failCount;
            // 更新当前flow失败节点个数
            executionFlowService.updateCurFault(flowId, totalFailCount);

            String logMsg = String.format("update flow fail count, current batch failCount: %s, total failCount: %s",
                    failCount, totalFailCount);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

            if (totalFailCount != 0 && totalFailCount > tolerance) {
                // 失败节点个数大于容错度, 更新flow状态为执行失败
                updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.ROLLBACK_FAILED);
                updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
                executionFlowService.updateFlow(updateExecutionFlowDTO);
                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                updateBmrFlowDto.setFlowId(flowId);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.FAILED_PAUSE);
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);

                String warnMsg = String.format("update flow status to FAIL_EXECUTE, because failed node count Greater than fault-tolerant failCount: %s, tolerance: %s", totalFailCount, tolerance);
                executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
                return;
            }
        }

        // 执行前一批
        Integer nextBatchId = currentBatchId - 1;

        if (nextBatchId >= minExecBatchId) {
            executionFlowService.updateCurrentBatchIdByFlowId(flowId, nextBatchId);
            String msg = String.format("flow minBatchId less than nextBatchId, will continue execute next batch, nextBatchId: %s, minBatchId: %s", nextBatchId, minExecBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            return;
        } else if (nextBatchId > 1) {
            String msg = String.format("flow minBatchId not less than nextBatchId, should pause this flow, nextBatchId: %s, minBatchId: %s", nextBatchId, minExecBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            return;
        } else if (currentBatchId <= 1) {
            updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
            updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.ROLLBACK_SUCCESS);
            String msg = String.format("current batch id is last one, flowId: %s, currentBatchId: %s, " +
                    "flow all node has rollback completed, update flow status to rollback success.", flowId, currentBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            executionFlowService.updateFlow(updateExecutionFlowDTO);

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            BeanUtils.copyProperties(updateExecutionFlowDTO, updateBmrFlowDto);
            updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.DONE_BUT_NOT_FINISH);
            updateBmrFlowDto.setFlowStatus(BmrFlowStatus.SUCCESS);
            bmrFlowService.alterFlowStatus(updateBmrFlowDto);
        }
    }

    public void handleRollbackWithStaged(long flowId) {
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        Preconditions.checkState(FlowStatusEnum.IN_ROLLBACK.equals(executionFlow.getFlowStatus()),
                "工作流状态需要为: [IN_ROLLBACK] 状态，当前为: " + executionFlow.getFlowStatus());
        executionFlowService.updateFlowRollbackType(FlowRollbackType.STAGE, flowId);
        Integer curBatchId = executionFlow.getCurBatchId();

        final ExecutionNodeEntity queryDo = new ExecutionNodeEntity();
        queryDo.setNodeType(null);
        queryDo.setFlowId(flowId);
        queryDo.setBatchId(curBatchId);
        final ExecutionNodeEntity nodeEntity = executionNodeService.queryOneNode(queryDo);
        log.info("handleRollbackWithStaged node is {}", JSONUtil.toJsonStr(nodeEntity));
        if (nodeEntity.getExecType().isRollbackState() && !nodeEntity.getNodeType().isNormalExecNode()) {
            if (curBatchId > 1) {
                curBatchId--;
                log.info("handleRollbackWithStaged with current batch id to: {}.", curBatchId);
                executionFlowService.updateCurrentBatchIdByFlowId(flowId, curBatchId);
            } else {
                executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.ROLLBACK_SUCCESS);
                return;
            }
        }

        final String curStage = executionNodeService.queryCurStage(flowId, curBatchId);
        log.info("handleRollbackWithStaged with current stage: {}.", curStage);

        final LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0)
                .le(ExecutionNodeEntity::getBatchId, curBatchId)
                .eq(ExecutionNodeEntity::getExecStage, curStage)
                .eq(ExecutionNodeEntity::getExecType, NodeExecType.FORWARD)
                .set(ExecutionNodeEntity::getExecType, NodeExecType.WAITING_ROLLBACK);
        executionNodeService.update(updateWrapper);
    }

    public void handleRollbackWithFullList(long flowId) {
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        Preconditions.checkState(FlowStatusEnum.IN_ROLLBACK.equals(executionFlow.getFlowStatus()),
                "工作流状态需要为: [IN_ROLLBACK] 状态，当前为: " + executionFlow.getFlowStatus());
        executionFlowService.updateFlowRollbackType(FlowRollbackType.GLOBAL, flowId);
        final Integer curBatchId = executionFlow.getCurBatchId();
        final LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0)
                .le(ExecutionNodeEntity::getBatchId, curBatchId)
                .eq(ExecutionNodeEntity::getExecType, NodeExecType.FORWARD)
                .set(ExecutionNodeEntity::getExecType, NodeExecType.WAITING_ROLLBACK);
        executionNodeService.update(updateWrapper);
    }

}
