package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkPeripheryComponentDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.VersionLockState;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionUpdateReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class SparkPeripheryComponentLockUpdateEventHandler extends AbstractBranchedTaskEventHandler {

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
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        final Long nodeId = executionJob.getId();
        SparkPeripheryComponentDeployJobExtParams deployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkPeripheryComponentDeployJobExtParams.class);
        if (Objects.isNull(deployJobExtParams)) {
            logPersist(taskEvent, "deployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        String jobId = executionJob.getNodeName();
        final ExecutionFlowEntity flowEntity = executionFlowService.getById(executionJob.getFlowId());
        final FlowDeployType deployType = flowEntity.getDeployType();
        VersionLockState lockState;
        switch (deployType) {
            case SPARK_PERIPHERY_COMPONENT_LOCK:
                lockState = VersionLockState.REQUIRE_LOCK;
                break;
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                lockState = VersionLockState.REQUIRE_UNLOCK;
                break;
            default:
                throw new IllegalArgumentException("un-support deploy type: " + deployType);
        }
        final SparkPeripheryComponentVersionUpdateReq versionUpdateReq = new SparkPeripheryComponentVersionUpdateReq();
        versionUpdateReq.setJobId(jobId);
        versionUpdateReq.setComponent(deployJobExtParams.getPeripheryComponent());
        versionUpdateReq.setLockState(lockState);

        boolean isSuc = sparkManagerService.updateSparkPeripheryComponentVersion(versionUpdateReq);
        String logContent = String.format("jobId %s update spark periphery component [%s] version lock state to [%s], result is %s",
                jobId, deployJobExtParams.getPeripheryComponent(), lockState, isSuc ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, logContent);
        return isSuc;
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     * @param taskEvent
     * @return
     */
    public boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) throws Exception {
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        final Long nodeId = executionJob.getId();
        SparkPeripheryComponentDeployJobExtParams deployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkPeripheryComponentDeployJobExtParams.class);
        if (Objects.isNull(deployJobExtParams)) {
            logPersist(taskEvent, "deployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        String jobId = executionJob.getNodeName();
        VersionLockState lockState = deployJobExtParams.isLocked() ? VersionLockState.REQUIRE_LOCK : VersionLockState.REQUIRE_UNLOCK;

        final SparkPeripheryComponentVersionUpdateReq versionUpdateReq = new SparkPeripheryComponentVersionUpdateReq();
        versionUpdateReq.setJobId(jobId);
        versionUpdateReq.setComponent(deployJobExtParams.getPeripheryComponent());
        versionUpdateReq.setLockState(lockState);

        boolean isSuc = sparkManagerService.updateSparkPeripheryComponentVersion(versionUpdateReq);
        String logContent = String.format("jobId %s rollback spark periphery component [%s] version lock state to [%s], result is %s",
                jobId, deployJobExtParams.getPeripheryComponent(), lockState, isSuc ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, logContent);
        return isSuc;
    }


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_VERSION_UPDATE;
    }
}
