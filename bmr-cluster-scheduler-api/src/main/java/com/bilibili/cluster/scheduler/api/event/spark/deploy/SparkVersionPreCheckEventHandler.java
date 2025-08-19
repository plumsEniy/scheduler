package com.bilibili.cluster.scheduler.api.event.spark.deploy;


import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 跳过逻辑节点
 */
@Slf4j
@Component
public class SparkVersionPreCheckEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Override
    public boolean hasRollbackBranch() {
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_DEPLOY_PRE_CHECK;
    }

    /**
     * 处理普通节点，并处于回滚状态的逻辑
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean executeNormalNodeOnRollbackStateTaskEvent(TaskEvent taskEvent) {
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        final Long nodeId = executionJob.getId();
        SparkDeployJobExtParams sparkDeployJobExtParams = executionNodePropsService.queryNodePropsByNodeId(nodeId, SparkDeployJobExtParams.class);
        if (Objects.isNull(sparkDeployJobExtParams)) {
            logPersist(taskEvent, "sparkDeployJobExtParams is null, nodeId is: " + nodeId);
            return false;
        }
        if (sparkDeployJobExtParams.isLockSparkVersion()) {
            logPersist(taskEvent, "job version is locked, skip...");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
            taskEvent.setOperationResult(NodeOperationResult.SPARK_JOB_VERSION_LOCKED);
            return true;
        }
        final String oldSparkVersion = sparkDeployJobExtParams.getOldSparkVersion();
        String targetSparkVersion = sparkDeployJobExtParams.getTargetSparkVersion();
        String message = String.format("will rollback spark version from [%s] to [%s]", targetSparkVersion, oldSparkVersion);
        logPersist(taskEvent, message);
        return true;
    }

    /**
     * 处理普通节点，并处于正向处理的逻辑
     * @param taskEvent
     * @return
     */
    @Override
    public boolean executeNormalNodeOnForwardStateTaskEvent(TaskEvent taskEvent) {
        final Long flowId = taskEvent.getFlowId();
        SparkDeployFlowExtParams flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, SparkDeployFlowExtParams.class);

        SparkDeployJobExtParams sparkDeployJobExtParams = new SparkDeployJobExtParams();
        final Long nodeId = taskEvent.getEventEntity().getExecutionNodeId();

        sparkDeployJobExtParams.setNodeId(nodeId);
        ExecutionNodeEntity executionJob = taskEvent.getExecutionNode();
        String jobId = executionJob.getNodeName();
        final SparkJobInfoDTO sparkJobInfoDTO = sparkManagerService.querySparkJobInfo(jobId,
                ExperimentJobType.COMPASS_JOB, Constants.DEFAULT_PADDING_LONG_VALUE);
        sparkDeployJobExtParams.setOldSparkVersion(sparkJobInfoDTO.getTargetSparkVersion());
        sparkDeployJobExtParams.setTargetSparkVersion(flowExtParams.getTargetSparkVersion());
        sparkDeployJobExtParams.setJobName(sparkJobInfoDTO.getJobName());
        sparkDeployJobExtParams.setJobId(sparkJobInfoDTO.getJobId());
        sparkDeployJobExtParams.setLockSparkVersion(sparkJobInfoDTO.isLockSparkVersion());
        logPersist(taskEvent, "jobExtParams is: " + JSONUtil.toJsonStr(sparkDeployJobExtParams));
        executionNodePropsService.saveNodeProp(nodeId, sparkDeployJobExtParams);

        if (sparkDeployJobExtParams.isLockSparkVersion()) {
            logPersist(taskEvent, "job version is locked, skip...");
            taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
            taskEvent.setOperationResult(NodeOperationResult.SPARK_JOB_VERSION_LOCKED);
            return true;
        }
        String message = String.format("will set spark version from [%s] to [%s]",
                sparkDeployJobExtParams.getOldSparkVersion(), sparkDeployJobExtParams.getTargetSparkVersion());
        logPersist(taskEvent, message);
        return true;
    }

}
