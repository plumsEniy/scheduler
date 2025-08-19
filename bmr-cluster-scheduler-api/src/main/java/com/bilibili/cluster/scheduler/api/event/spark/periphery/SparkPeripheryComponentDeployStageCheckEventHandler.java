package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class SparkPeripheryComponentDeployStageCheckEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    BmrMetadataService bmrMetadataService;

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
        }

        if (rollbackType.equals(FlowRollbackType.GLOBAL) && flowEntity.getDeployType() == FlowDeployType.SPARK_PERIPHERY_COMPONENT_DEPLOY) {
            message = String.format("执行全量回滚,该逻辑节点%s是当前阶段%s首个节点", executionNodeId, execStage);
            logPersist(taskEvent, message);
            final String minStage = executionNodeService.queryMinStageByFlowId(flowId);
            if (minStage.equals(execStage)) {
                final String releaseScopeTypeValue = flowEntity.getReleaseScopeType();
                FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
                if (flowReleaseScopeType == FlowReleaseScopeType.FULL_RELEASE) {
                    final SparkPeripheryComponentDeployFlowExtParams deployFlowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkPeripheryComponentDeployFlowExtParams.class);
                    final String originalVersion = deployFlowExtParams.getOriginalVersion();
                    final SparkPeripheryComponent peripheryComponent = deployFlowExtParams.getPeripheryComponent();
                    message = String.format("全量回滚完成，设置原版本[%s]为spark组件[%s]默认版本", originalVersion, deployFlowExtParams.getPeripheryComponent());
                    logPersist(taskEvent, message);
                    Long originalPackageId = 0L;

                    if (!StringUtils.isBlank(originalVersion)) {
                        InstallationPackage originalPackage = bmrMetadataService.queryPackageByMinorVersion(originalVersion);
                        if (Objects.isNull(originalPackage)) {
                            throw new IllegalArgumentException("can not find original package by minor version, minor version is " + originalVersion);
                        }
                        originalPackageId = originalPackage.getId();
                    }
                    if (originalPackageId > 0) {
                        bmrMetadataService.updateDefaultVersion(originalPackageId, peripheryComponent.name());
                        message = String.format("变更spark组件[%s]默认安装包成功，安装包版本id%s", peripheryComponent, originalPackageId);
                    } else {
                        bmrMetadataService.removeDefaultPackage(peripheryComponent.name());
                        message = String.format("spark组件[%s]原安装包不存在，消除默认安装包", peripheryComponent);
                    }
                } else {
                    message = String.format("全量回滚完成。");
                }
                logPersist(taskEvent, message);
            }
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
            // 判断是否为全量发布
            final ExecutionFlowEntity flowEntity = executionFlowService.queryByIdWithTransactional(flowId);
            final String releaseScopeTypeValue = flowEntity.getReleaseScopeType();
            FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
            message = "发布范围：" + flowReleaseScopeType.getDesc();
            logPersist(taskEvent, message);
            if (flowReleaseScopeType == FlowReleaseScopeType.FULL_RELEASE && flowEntity.getDeployType() == FlowDeployType.SPARK_PERIPHERY_COMPONENT_DEPLOY) {
                final SparkPeripheryComponentDeployFlowExtParams deployFlowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkPeripheryComponentDeployFlowExtParams.class);
                final String targetVersion = deployFlowExtParams.getTargetVersion();
                final SparkPeripheryComponent peripheryComponent = deployFlowExtParams.getPeripheryComponent();
                message = String.format("全量发布完成，设置发布版本[%s]为spark组件[%s]默认版本", targetVersion, peripheryComponent);
                logPersist(taskEvent, message);
                InstallationPackage targetPackage = bmrMetadataService.queryPackageByMinorVersion(targetVersion);
                if (Objects.isNull(targetPackage)) {
                    throw new IllegalArgumentException("can not find target package by minor version, minor version is " + targetVersion);
                }
                bmrMetadataService.updateDefaultVersion(targetPackage.getId(), peripheryComponent.name());
                message = String.format("变更spark组件[%s]默认安装包成功，安装包版本id%s", peripheryComponent, targetPackage.getId());
                logPersist(taskEvent, message);
            }
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
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_STAGE_CHECK;
    }

}
