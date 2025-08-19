package com.bilibili.cluster.scheduler.api.event.presto.experiment;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.api.service.experiment.DolphinPlusService;
import com.bilibili.cluster.scheduler.api.service.experiment.ExperimentJobService;
import com.bilibili.cluster.scheduler.api.service.scheduler.DolphinSchedulerInteractService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentExecutionStatus;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.QueryExperimentRequest;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TrinoExperimentQueryEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    DolphinPlusService dolphinPlusService;

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    DolphinSchedulerInteractService dolphinSchedulerInteractService;

    @Resource
    ExperimentJobService experimentJobService;

    @Value("${dolphin.dqc.token}")
    private String dqcToken;

    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("2")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean executeNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        try {
            MDC.put(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY, dqcToken);
            Long executionNodeId = taskEvent.getExecutionNodeId();
            final ExperimentJobProps jobProps = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, ExperimentJobProps.class);
            final String experimentId = jobProps.getExperimentId();

            if (StringUtils.isBlank(experimentId)) {
                logPersist(taskEvent, "query experiment result,but experimentId is blank.");
                return false;
            } else {
                logPersist(taskEvent, "query experiment result, experimentId is: " + experimentId);
            }

            QueryExperimentRequest request = new QueryExperimentRequest();
            request.setExperimentId(experimentId);
            request.setPlatformSource(jobProps.getPlatformA());
            final ExperimentType experimentType = jobProps.getExperimentType();
            switch (experimentType) {
                case COMPARATIVE_TASK:
                    request.setPlatformTarget(jobProps.getPlatformB());
                    break;
                case PERFORMANCE_TEST:
                    request.setPlatformTarget("empty_default");
                    break;
            }
            logPersist(taskEvent, "query experiment params is: " + JSONUtil.toJsonStr(request));
            long startTs = jobProps.getStartTs();

            int currentQueryErrorCnt = 0;
            int maxQueryErrorCnt = 5;
            boolean awareDolphinDetailUrl = false;

            while (true) {
                ThreadUtil.sleep(Constants.ONE_MINUTES);

                ExperimentJobResult experimentJobResult;
                try {
                    experimentJobResult = dolphinPlusService.queryExperimentResult(request);
                    currentQueryErrorCnt = 0;
                } catch (Exception e) {
                    currentQueryErrorCnt++;
                    if (currentQueryErrorCnt >= maxQueryErrorCnt) {
                        logPersist(taskEvent,
                                "experiment [FAILURE] when query result over than maxQueryErrorCnt[5] times, latest cause: "
                                        + e.getMessage());
                        return false;
                    }
                    continue;
                }

                final String workflowInstanceUrl = experimentJobResult.getWorkflowInstanceUrl();
                if (!StringUtils.isBlank(workflowInstanceUrl)) {
                    if (!awareDolphinDetailUrl) {
                        logPersist(taskEvent, "dolphin scheduler detail url is:\n " + workflowInstanceUrl);
                        awareDolphinDetailUrl = true;
                    }
                }

                String executionStatus = experimentJobResult.getExecutionStatus();

                if (ExperimentExecutionStatus.isWaitingCost(executionStatus)) {
                    logPersist(taskEvent, "experiment [WAITING_COST] with result :\n" + JSONUtil.toJsonStr(experimentJobResult));
                    // update spark manager ci job info
                    ExperimentJobResultDTO jobResultDTO = experimentJobService.transferToWaitingCostJobResultDTO(jobProps, experimentJobResult, executionNodeId);
                    jobResultDTO.setExecNodeId(executionNodeId);
                    sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
                    return true;
                }


                if (ExperimentExecutionStatus.isSuccess(executionStatus)) {
                    logPersist(taskEvent, "experiment [SUCCESS] with result :\n" + JSONUtil.toJsonStr(experimentJobResult));
                    // update spark manager ci job info
                    ExperimentJobResultDTO jobResultDTO = experimentJobService.transferToSuccessJobResultDTO(jobProps, experimentJobResult, executionNodeId);
                    jobResultDTO.setExecNodeId(executionNodeId);
                    sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
                    return true;
                }

                if (ExperimentExecutionStatus.isFailure(executionStatus)) {
                    logPersist(taskEvent, "experiment [FAILURE] with result :\n" + JSONUtil.toJsonStr(experimentJobResult));
                    ExperimentJobResultDTO jobResultDTO = experimentJobService.transferToFailureJobResultDTO(jobProps, experimentJobResult, executionNodeId);
                    jobResultDTO.setExecNodeId(executionNodeId);
                    sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
                    return false;
                }

                long currentTs = System.currentTimeMillis();
                if (currentTs - startTs > TimeUnit.DAYS.toMillis(1)) {
                    logPersist(taskEvent, "experiment [FAILURE] case by consume time over than a day ");
                    experimentJobService.updateSparkCiJobToFailure(jobProps, experimentJobResult, executionNodeId);
                    return false;
                }
            }
        } finally {
            MDC.remove(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY);
        }
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.TRINO_EXPERIMENT_QUERY_EXEC_EVENT;
    }


}
