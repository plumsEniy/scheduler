package com.bilibili.cluster.scheduler.api.event.spark.periphery;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkPeripheryComponentDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.req.SparkPeripheryComponentVersionUpdateReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Component
public class SparkPeripheryComponentDeployUpdateVersionEventHandler extends AbstractBranchedTaskEventHandler {

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
        final String targetVersion = deployJobExtParams.getTargetVersion();
        final SparkPeripheryComponentVersionUpdateReq versionUpdateReq = new SparkPeripheryComponentVersionUpdateReq();
        versionUpdateReq.setJobId(jobId);
        versionUpdateReq.setComponent(deployJobExtParams.getPeripheryComponent());
        versionUpdateReq.setTargetVersion(targetVersion);

        boolean isSuc = sparkManagerService.updateSparkPeripheryComponentVersion(versionUpdateReq);
        String logContent = String.format("jobId %s set spark periphery component [%s] version to [%s], result is %s",
                jobId, deployJobExtParams.getPeripheryComponent(), targetVersion, isSuc ? "SUCCESS" : "FAILURE");
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
        String originalVersion = deployJobExtParams.getOriginalVersion();
        // 重置无版本状态
        if (!StringUtils.hasText(originalVersion)) {
            originalVersion = Constants.NAN;
        }

        final SparkPeripheryComponentVersionUpdateReq versionUpdateReq = new SparkPeripheryComponentVersionUpdateReq();
        versionUpdateReq.setJobId(jobId);
        versionUpdateReq.setComponent(deployJobExtParams.getPeripheryComponent());
        versionUpdateReq.setTargetVersion(originalVersion);

        boolean isSuc = sparkManagerService.updateSparkPeripheryComponentVersion(versionUpdateReq);
        String logContent = String.format("jobId %s rollback spark periphery component [%s] version to [%s], result is %s",
                jobId, deployJobExtParams.getPeripheryComponent(), originalVersion, isSuc ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, logContent);
        return isSuc;
    }


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_UPDATE_VERSION;
    }

}
