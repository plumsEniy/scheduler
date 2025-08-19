package com.bilibili.cluster.scheduler.api.service.flow.rollback.phase;

import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.FlowRollbackFactory;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractNewPhasedFlowRollbackFactory implements FlowRollbackFactory {

    @Resource
    protected ExecutionFlowService flowService;

    @Resource
    protected ExecutionFlowPropsService flowPropsService;

    @Resource
    protected ExecutionNodeService nodeService;

    @Resource
    protected ExecutionNodePropsService nodePropsService;

    @Resource
    protected ExecutionNodeEventService nodeEventService;

    @Resource
    protected BmrFlowService bmrFlowService;

    @Resource
    protected ExecutionLogService logService;

    @Override
    public List<FlowOperateButtonEnum> getRollbackButton(ExecutionFlowEntity flowEntity) {
        return Arrays.asList(FlowOperateButtonEnum.FULL_ROLLBACK);
    }


    // 按需实现-处理还未处于回滚状态的节点列表
    protected void handlerUnRollbackNodes(ExecutionFlowEntity flowEntity) {

    }

    @Override
    public void handlerUpdateFlowStatus(ExecutionFlowEntity flowEntity) {
        // 处理待回滚节点
        handlerUnRollbackNodes(flowEntity);
        final Long flowId = flowEntity.getId();

        final Integer currentBatchId = flowEntity.getCurBatchId();

        List<NodeExecuteStatusEnum> nodeStatus = new ArrayList<>();
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);
        List<ExecutionNodeEntity> executionUnNodeEntities = nodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
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
        List<ExecutionNodeEntity> executionRunningNodeEntities = nodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
        if (!CollectionUtils.isEmpty(executionRunningNodeEntities)) {
            // 当前批次有任务处于执行中, 更新flow状态为执行中
            // executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.IN_ROLLBACK);

            String logMsg = String.format("current batch id: %s has node status is IN_ROLLBACK", currentBatchId);
            logService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);
            return;
        }

        // 清空
        nodeStatus.clear();
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE);
        List<ExecutionNodeEntity> executionFailNodeEntities = nodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);

        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setRollbackType(FlowRollbackType.GLOBAL);
        updateExecutionFlowDTO.setFlowId(flowId);

        if (!CollectionUtils.isEmpty(executionFailNodeEntities)) {
            final Integer tolerance = flowEntity.getTolerance();
            // 当前批次失败节点数
            int failCount = executionFailNodeEntities.size();
            int totalFailCount = flowEntity.getCurFault() + failCount;
            // 更新当前flow失败节点个数
            flowService.updateCurFault(flowId, totalFailCount);

            String logMsg = String.format("update flow fail count, current batch failCount: %s, total failCount: %s",
                    failCount, totalFailCount);
            logService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

            if (totalFailCount != 0 && totalFailCount > tolerance) {
                // 失败节点个数大于容错度, 更新flow状态为执行失败
                updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.ROLLBACK_FAILED);
                updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
                flowService.updateFlow(updateExecutionFlowDTO);
                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                updateBmrFlowDto.setFlowId(flowId);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.FAILED_PAUSE);
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);

                String warnMsg = String.format("update flow status to FAIL_EXECUTE, because failed node count Greater than fault-tolerant failCount: %s, tolerance: %s", totalFailCount, tolerance);
                logService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
                return;
            }
        }

        // 执行后一批
        Integer nextBatchId = currentBatchId + 1;
        int maxBatchId = nodeService.queryMaxBatchId(flowId);

        if (nextBatchId <= maxBatchId) {
            flowService.updateCurrentBatchIdByFlowId(flowId, nextBatchId);
            String msg = String.format("flow minBatchId less than nextBatchId, will continue execute next batch, nextBatchId: %s, maxBatchId: %s", nextBatchId, maxBatchId);
            logService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            return;
        } else {
            updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
            updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.ROLLBACK_SUCCESS);
            String msg = String.format("current batch id is the latest, flowId: %s, currentBatchId: %s, " +
                    "flow all node has rollback completed, update flow status to rollback success, ", flowId, currentBatchId);
            logService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            flowService.updateFlow(updateExecutionFlowDTO);

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            BeanUtils.copyProperties(updateExecutionFlowDTO, updateBmrFlowDto);
            updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.DONE_BUT_NOT_FINISH);
            updateBmrFlowDto.setFlowStatus(BmrFlowStatus.SUCCESS);
            bmrFlowService.alterFlowStatus(updateBmrFlowDto);
        }
    }
}
