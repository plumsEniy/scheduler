package com.bilibili.cluster.scheduler.api.event.spark.deploy;

import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class SparkVersionReleaseEventHandler extends AbstractTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        String jobId = executionNode.getNodeName();

        String msg = String.format("job id of %s spark version will un-lock", jobId);
        logPersist(taskEvent, msg);
        saveJobProps(jobId, executionNode.getId());

        boolean isSuccess = sparkManagerService.lockSparkJobVersion(jobId, false);
        msg = "spark version release : " + (isSuccess ? "SUCCESS" : "FAILURE");
        logPersist(taskEvent, msg);
        return isSuccess;
    }

    private void saveJobProps(String jobId, Long nodeId) {
        SparkJobInfoDTO jobInfoDTO = sparkManagerService.querySparkJobInfo(jobId,
                ExperimentJobType.COMPASS_JOB, Constants.DEFAULT_PADDING_LONG_VALUE);
        SparkDeployJobExtParams jobExtParams = new SparkDeployJobExtParams();
        jobExtParams.setJobId(jobId);
        jobExtParams.setJobName(jobInfoDTO.getJobName());
        jobExtParams.setTargetSparkVersion(jobInfoDTO.getTargetSparkVersion());
        jobExtParams.setNodeId(nodeId);
        executionNodePropsService.saveNodeProp(nodeId, jobExtParams);
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.SPARK_VERSION_RELEASE_EXEC_EVENT;
    }

}
