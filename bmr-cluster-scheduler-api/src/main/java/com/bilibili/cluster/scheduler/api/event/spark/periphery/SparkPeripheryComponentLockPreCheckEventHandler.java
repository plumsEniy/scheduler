package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkPeripheryComponentDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionInfoReq;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp.SparkPeripheryComponentVersionInfoDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class SparkPeripheryComponentLockPreCheckEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Override
    public boolean hasRollbackBranch() {
        return true;
    }

    /**
     * 处理普通节点，并处于正向处理的逻辑
     * @param taskEvent
     * @return
     */
    public boolean executeNormalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String jobId = executionNode.getNodeName();
        final Long flowId = taskEvent.getFlowId();
        final SparkPeripheryComponentDeployFlowExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkPeripheryComponentDeployFlowExtParams.class);
        final SparkPeripheryComponent peripheryComponent = flowExtParams.getPeripheryComponent();
        final SparkPeripheryComponentDeployJobExtParams jobExtParams = new SparkPeripheryComponentDeployJobExtParams();
        jobExtParams.setJobId(jobId);
        jobExtParams.setPeripheryComponent(peripheryComponent);
        final SparkPeripheryComponentVersionInfoReq req = new SparkPeripheryComponentVersionInfoReq(peripheryComponent, jobId);
        final SparkPeripheryComponentVersionInfoDTO componentVersionInfoDTO = sparkManagerService.querySparkPeripheryComponentVersionInfo(req);
        jobExtParams.setNodeId(executionNode.getId());
        jobExtParams.setJobName(componentVersionInfoDTO.getJobName());
        jobExtParams.setOriginalVersion(componentVersionInfoDTO.getTargetVersion());
        jobExtParams.setTargetVersion(flowExtParams.getTargetVersion());
        jobExtParams.setLocked(componentVersionInfoDTO.isLocked());
        logPersist(taskEvent, "jobExtParams is: " + JSONUtil.toJsonStr(jobExtParams));
        // save job ext props
        executionNodePropsService.saveNodeProp(executionNode.getId(), jobExtParams);

        final ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);

        final FlowDeployType deployType = flowEntity.getDeployType();
        switch (deployType) {
            case SPARK_PERIPHERY_COMPONENT_LOCK:
                if (componentVersionInfoDTO.isLocked()) {
                    logPersist(taskEvent, "periphery component version already locked, skip...");
                    taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
                    taskEvent.setOperationResult(NodeOperationResult.SPARK_PERIPHERY_COMPONENT_VERSION_LOCKED);
                } else {
                    String message = String.format("will update spark periphery component [%s] lock state from [unlock] to [locked]",
                            peripheryComponent);
                    logPersist(taskEvent, message);
                }
                return true;
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                if (componentVersionInfoDTO.isLocked()) {
                    String message = String.format("will update spark periphery component [%s] lock state from [locked] to [unlock]",
                            peripheryComponent);
                    logPersist(taskEvent, message);
                } else {
                    logPersist(taskEvent, "periphery component version already unlock, skip...");
                    taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
                }
                return true;
            default:
                throw new IllegalArgumentException("un-support of deploy type: " + deployType);
        }
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     * @param taskEvent
     * @return
     */
    public boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final Long nodeId = executionNode.getId();
        final SparkPeripheryComponentDeployJobExtParams deployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkPeripheryComponentDeployJobExtParams.class);
        if (Objects.isNull(deployJobExtParams)) {
            logPersist(taskEvent, "deployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        String message = String.format("periphery component of [%s] will rollback lock state to ",
                deployJobExtParams.getPeripheryComponent(),
                deployJobExtParams.isLocked() ? "locked" : "unlock");
        logPersist(taskEvent, message);
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_PRE_CHECK;
    }
}
