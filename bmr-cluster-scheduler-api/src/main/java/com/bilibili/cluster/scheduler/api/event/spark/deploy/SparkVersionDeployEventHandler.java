package com.bilibili.cluster.scheduler.api.event.spark.deploy;

import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployJobExtParams;
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
public class SparkVersionDeployEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    protected boolean hasRollbackBranch() {
        return true;
    }

    /**
     * 处理普通节点，并处于正向处理的逻辑
     * @param taskEvent
     * @return
     */
    @Override
    public boolean executeNormalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) throws Exception {
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        final Long nodeId = executionJob.getId();
        SparkDeployJobExtParams sparkDeployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkDeployJobExtParams.class);
        if (Objects.isNull(sparkDeployJobExtParams)) {
            logPersist(taskEvent, "sparkDeployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        String jobId = executionJob.getNodeName();
        final String targetSparkVersion = sparkDeployJobExtParams.getTargetSparkVersion();
        boolean isSuc = sparkManagerService.updateSparkVersion(jobId, targetSparkVersion);
        String logContent = String.format("jobId %s set spark version to %s, result is %s",
                jobId, targetSparkVersion, isSuc ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, logContent);
        return isSuc;
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     * @param taskEvent
     * @return
     */
    @Override
    public  boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) throws Exception {
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        final Long nodeId = executionJob.getId();
        SparkDeployJobExtParams sparkDeployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkDeployJobExtParams.class);
        if (Objects.isNull(sparkDeployJobExtParams)) {
            logPersist(taskEvent, "sparkDeployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        String jobId = executionJob.getNodeName();
        String oldSparkVersion = sparkDeployJobExtParams.getOldSparkVersion();
        // 重置无版本状态
        if (!StringUtils.hasText(oldSparkVersion)) {
            oldSparkVersion = Constants.NAN;
        }
        boolean isSuc = sparkManagerService.updateSparkVersion(jobId, oldSparkVersion);
        String logContent = String.format("jobId %s rollback spark version to %s, result is %s",
                jobId, oldSparkVersion, isSuc ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, logContent);
        return isSuc;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_DEPLOY_EXEC_EVENT;
    }
}
