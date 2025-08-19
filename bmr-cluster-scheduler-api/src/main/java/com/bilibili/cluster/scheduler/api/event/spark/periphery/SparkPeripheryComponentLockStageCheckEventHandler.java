package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SparkPeripheryComponentLockStageCheckEventHandler extends AbstractBranchedTaskEventHandler {

    // 是否存在回滚分支
    protected boolean hasRollbackBranch() {
        return true;
    }

    // 是否跳过逻辑节点
    protected boolean skipLogicalNode() {
        return false;
    }

    // 是否跳过普通节点
    protected boolean skipNormalNode() {
        return true;
    }


    @Override
    public boolean executeLogicalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) {
        final Long flowId = taskEvent.getFlowId();
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final NodeType nodeType = executionNode.getNodeType();
        String message;
        // 仅处理 STAGE_START_NODE 类型节点
        if (!nodeType.equals(NodeType.STAGE_START_NODE)) {
            message = String.format("回滚中，当前节点类型为: [%s], 跳过执行.", nodeType.getDesc());
            logPersist(taskEvent, message);
            return true;
        }

        final Long executionNodeId = executionNode.getId();
        final String execStage = executionNode.getExecStage();
        final ExecutionFlowEntity flowEntity = executionFlowService.queryByIdWithTransactional(flowId);
        final FlowRollbackType rollbackType = flowEntity.getFlowRollbackType();

        if (rollbackType.equals(FlowRollbackType.STAGE)) {
            message = String.format("执行阶段回滚,该逻辑节点%s是当前阶段%s首个节点，将暂停发布任务。", executionNodeId, execStage);
            logPersist(taskEvent, message);
            executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.PAUSED);
            bmrFlowService.alterFlowStatus(flowId, FlowOperateButtonEnum.PAUSE);
            return true;
        } else {
            message = String.format("执行阶段回滚,该逻辑节点%s是当前阶段%s首个节点。", executionNodeId, execStage);
            logPersist(taskEvent, message);
        }
        return true;
    }

    /**
     * 处理逻辑节点，并处于正向处理的逻辑
     *
     * @param taskEvent
     * @return
     */
    @Override
    public boolean executeLogicalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) {
        final Long flowId = taskEvent.getFlowId();
        final String maxStage = executionNodeService.queryMaxStageByFlowId(flowId);
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final NodeType nodeType = executionNode.getNodeType();
        String message;
        // 仅处理 STAGE_END_NODE 类型节点
        if (!nodeType.equals(NodeType.STAGE_END_NODE)) {
            message = String.format("正向执行中，当前节点类型为: [%s], 跳过执行.", nodeType.getDesc());
            logPersist(taskEvent, message);
            return true;
        }

        final Long executionNodeId = executionNode.getId();
        final String execStage = executionNode.getExecStage();

        if (maxStage.equals(execStage)) {
            // last stage
            message = String.format("该逻辑节点%s是最终阶段%s的最后处理节点", executionNodeId, maxStage);
            logPersist(taskEvent, message);
        } else {
            final SparkPeripheryComponentDeployFlowExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkPeripheryComponentDeployFlowExtParams.class);
            if (flowExtParams.isSkipStagePause()) {
                message = String.format("该逻辑节点%s是当前阶段%s最后节点。", executionNodeId, execStage);
                logPersist(taskEvent, message);
            } else {
                message = String.format("该逻辑节点%s是当前阶段%s最后节点，将暂停发布任务。", executionNodeId, execStage);
                logPersist(taskEvent, message);
                executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.PAUSED);
                bmrFlowService.alterFlowStatus(flowId, FlowOperateButtonEnum.PAUSE);
            }
        }
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_STAGE_CHECK;
    }
}
