package com.bilibili.cluster.scheduler.api.service.experiment;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.scheduler.DolphinSchedulerInteractService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobStatus;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.TaskInstance;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentExecutionStatus;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.QueryExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResult;
import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinWorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExperimentJobServiceImpl implements ExperimentJobService {

    @Resource
    ExecutionLogService executionLogService;

    @Resource
    DolphinSchedulerInteractService dolphinSchedulerInteractService;

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    DolphinPlusService dolphinPlusService;

    @Resource
    ExecutionNodePropsService executionNodePropsService;

    @Value("${dolphin.dqc.token}")
    private String dqcToken;

    @Override
    public void checkTranslateResultTableName(SqlTranslateResult sqlTranslateResult, TaskEvent taskEvent) {
        final String sourceEngineTargetTable = sqlTranslateResult.getSourceEngineTargetTable();
        final String targetEngineTargetTable = sqlTranslateResult.getTargetEngineTargetTable();
        if (StringUtils.isBlank(sourceEngineTargetTable) || StringUtils.isBlank(targetEngineTargetTable)) {
            String resultJson = JSONUtil.toJsonStr(sqlTranslateResult);
            logPersist(taskEvent, "[ERROR] sql translate with result: " + resultJson);
            throw new IllegalArgumentException("sql translate error, result is : " + resultJson);
        }
        boolean isUseTmpDb = true;
        if (!sourceEngineTargetTable.startsWith("tmp_")) {
            isUseTmpDb = false;
            logPersist(taskEvent, "[ERROR] sql translate use source db is " + sourceEngineTargetTable + ", require tmp DB.");
        }
        if (!targetEngineTargetTable.startsWith("tmp_")) {
            isUseTmpDb = false;
            logPersist(taskEvent, "[ERROR] sql translate use target db is " + targetEngineTargetTable + ", require tmp DB.");
        }
        if (!isUseTmpDb) {
            throw new IllegalArgumentException("through sql translate not use tmp DB.");
        }
    }

    @Override
    public ExperimentJobResultDTO transferToFailureJobResultDTO(ExperimentJobProps jobProps,
                                                                 ExperimentJobResult experimentJobResult, Long executionNodeId) {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
        jobResultDTO.setJobId(jobProps.getJobId());
        jobResultDTO.setExecNodeId(executionNodeId);
        jobResultDTO.setJobStatus(ExperimentJobStatus.FAIL);
        final ExperimentType experimentType = jobProps.getExperimentType();

        final String workflowInstanceUrl = experimentJobResult.getWorkflowInstanceUrl();
        try {
            fillJobResultByDetailUrl(workflowInstanceUrl, jobResultDTO);
            jobResultDTO.setAApplicationId(experimentJobResult.getEngineQueryIdA());
            if (experimentType == ExperimentType.COMPARATIVE_TASK) {
                jobResultDTO.setBApplicationId(experimentJobResult.getEngineQueryIdB());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jobResultDTO;
    }

    @Override
    public void updateSparkCiJobToFailure(ExperimentJobProps jobProps, ExperimentJobResult experimentJobResult, long execNodeId) {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
        jobResultDTO.setJobId(jobProps.getJobId());
        jobResultDTO.setExecNodeId(execNodeId);
        jobResultDTO.setJobStatus(ExperimentJobStatus.FAIL);
        final ExperimentType experimentType = jobProps.getExperimentType();

        final String workflowInstanceUrl = experimentJobResult.getWorkflowInstanceUrl();
        try {
            fillJobResultByDetailUrl(workflowInstanceUrl, jobResultDTO);
            jobResultDTO.setAApplicationId(experimentJobResult.getEngineQueryIdA());
            if (experimentType == ExperimentType.COMPARATIVE_TASK) {
                jobResultDTO.setBApplicationId(experimentJobResult.getEngineQueryIdB());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
    }

    /**
     * 提供根据nodeId查询实验任务结果Api
     * @param execNodeId
     * @return
     */
    @Override
    public ExperimentJobResultDTO queryExperimentJobResultByExecNodeId(long execNodeId) {
        try {
            MDC.put(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY, dqcToken);
            final ExperimentJobProps jobProps = executionNodePropsService.queryNodePropsByNodeId(execNodeId, ExperimentJobProps.class);
            if (Objects.isNull(jobProps)) {
                throw new IllegalArgumentException("jobProps is not exists, exec node id is: " + execNodeId);
            }
            final String experimentId = jobProps.getExperimentId();
            if (StringUtils.isBlank(experimentId)) {
                throw new IllegalArgumentException("experimentId is not exists, exec node id is: " + execNodeId);
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
            ExperimentJobResult experimentJobResult = dolphinPlusService.queryExperimentResult(request);
            String executionStatus = experimentJobResult.getExecutionStatus();

            if (ExperimentExecutionStatus.isWaitingCost(executionStatus)) {
                return transferToWaitingCostJobResultDTO(jobProps, experimentJobResult, execNodeId);
            }

            if (ExperimentExecutionStatus.isSuccess(executionStatus)) {
                return transferToSuccessJobResultDTO(jobProps, experimentJobResult, execNodeId);
            }

            if (ExperimentExecutionStatus.isFailure(executionStatus)) {
                return transferToFailureJobResultDTO(jobProps, experimentJobResult, execNodeId);
            }

            return transferToRunningJobResultDTO(jobProps, experimentJobResult, execNodeId);

        } finally {
            MDC.remove(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY);
        }
    }

    @Override
    public ExperimentJobResultDTO transferToRunningJobResultDTO(ExperimentJobProps jobProps, ExperimentJobResult experimentJobResult, long execNodeId) {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
        jobResultDTO.setJobId(jobProps.getJobId());
        jobResultDTO.setExecNodeId(execNodeId);
        jobResultDTO.setJobStatus(ExperimentJobStatus.RUNNING);
        jobResultDTO.setJobDetail(experimentJobResult.getWorkflowInstanceUrl());
        jobResultDTO.setAApplicationId(experimentJobResult.getEngineQueryIdA());
        jobResultDTO.setBApplicationId(experimentJobResult.getEngineQueryIdB());
        return jobResultDTO;
    }

    @Override
    public ExperimentJobResultDTO transferToSuccessJobResultDTO(ExperimentJobProps jobProps, ExperimentJobResult experimentJobResult, long execNodeId) {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
        jobResultDTO.setJobId(jobProps.getJobId());
        jobResultDTO.setExecNodeId(execNodeId);
        jobResultDTO.setJobStatus(ExperimentJobStatus.SUCCESS);
        final LocalDateTime now = LocalDateTime.now();
        jobResultDTO.setAEndTime(now);

        final ExperimentType experimentType = jobProps.getExperimentType();
        final String platformA = jobProps.getPlatformA();
        final String platformB = jobProps.getPlatformB();
        final Map<String, Double> cpu = experimentJobResult.getCpu();
        final Map<String, Double> memory = experimentJobResult.getMemory();
        final Map<String, Double> duration = experimentJobResult.getDuration();
        final String workflowInstanceUrl = experimentJobResult.getWorkflowInstanceUrl();
        fillJobResultByDetailUrl(workflowInstanceUrl, jobResultDTO);
        jobResultDTO.setAApplicationId(experimentJobResult.getEngineQueryIdA());

        // metrics value
        Double defaultValue = 0.0d;
        final Double aCpuCost = cpu.getOrDefault(platformA, defaultValue);
        final Double bCpuCost = cpu.getOrDefault(platformB, defaultValue);
        final Double aMemoryCost = memory.getOrDefault(platformA, defaultValue);
        final Double bMemoryCost = memory.getOrDefault(platformB, defaultValue);
        final Double aDurationCost = duration.getOrDefault(platformA, defaultValue);
        final Double bDurationCost = duration.getOrDefault(platformB, defaultValue);

        jobResultDTO.setAResourceCpuUsage(aCpuCost);
        jobResultDTO.setAResourceMemUsage(formatMemoryToGB(aMemoryCost, defaultValue));
        jobResultDTO.setARunTime(aDurationCost);

        switch (experimentType) {
            case COMPARATIVE_TASK:
                jobResultDTO.setBEndTime(now);
                jobResultDTO.setBResourceCpuUsage(bCpuCost);
                jobResultDTO.setBResourceMemUsage(formatMemoryToGB(bMemoryCost, defaultValue));
                jobResultDTO.setBRunTime(bDurationCost);
                jobResultDTO.setBApplicationId(experimentJobResult.getEngineQueryIdB());
                break;
            case PERFORMANCE_TEST:
                jobResultDTO.setBResourceCpuUsage(null);
                jobResultDTO.setBResourceMemUsage(null);
                jobResultDTO.setBRunTime(null);
                break;
        }
        return jobResultDTO;
    }

    private Double formatMemoryToGB(Double memoryCost, Double defaultValue) {
        if (memoryCost.doubleValue() > defaultValue.doubleValue()) {
            DecimalFormat df = new DecimalFormat("#0.00");
            String memoryGB = df.format(memoryCost / 1024.0);
            return Double.parseDouble(memoryGB);
        } else {
            return defaultValue;
        }
    }

    private void fillJobResultByDetailUrl(String workflowInstanceUrl, ExperimentJobResultDTO jobResultDTO) {
        if (StringUtils.isBlank(workflowInstanceUrl)) {
            return;
        }
        jobResultDTO.setJobDetail(workflowInstanceUrl);
        // http://pre-bmr.scheduler.bilibili.co/#/projects/12969915754848/workflow/instances/41215
        String regexPattern = ".*\\/projects\\/(?<projectId>\\w+)\\/workflow\\/instances\\/(?<instanceId>\\w+)";

        Pattern urlRegexPattern = Pattern.compile(regexPattern);
        Matcher matcher = urlRegexPattern.matcher(workflowInstanceUrl);
        String projectId;
        String instanceId;
        if (matcher.matches()) {
            projectId = matcher.group("projectId");
            instanceId = matcher.group("instanceId");
        } else {
            throw new IllegalArgumentException("regexPattern is not matcher: " + workflowInstanceUrl);
        }

        MDC.put(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY, dqcToken);
        List<TaskInstance> taskInstanceList = dolphinSchedulerInteractService.queryTaskNodeListExecState(projectId, instanceId);
        if (CollectionUtils.isEmpty(taskInstanceList)) {
            return;
        }

        for (TaskInstance taskInstance : taskInstanceList) {
            final DolpTaskType taskType = taskInstance.getTaskType();
            String taskName = taskInstance.getName();
            final DolphinWorkflowExecutionStatus instanceState = taskInstance.getState();
            final String startTimeValue = taskInstance.getStartTime();
            final String endTimeValue = taskInstance.getEndTime();

            LocalDateTime startTime = StringUtils.isBlank(startTimeValue) ? null :
                    LocalDateFormatterUtils.parseByPattern(Constants.FMT_DATE_TIME, startTimeValue);
            LocalDateTime endTime = StringUtils.isBlank(endTimeValue) ? null :
                    LocalDateFormatterUtils.parseByPattern(Constants.FMT_DATE_TIME, endTimeValue);

            switch (taskType) {
                case BILIBILI_SPARK:
                    if (taskName.equals("sparkTaskA")) {
                        jobResultDTO.setAStartTime(startTime);
                        jobResultDTO.setAEndTime(endTime);
                    }
                    if (taskName.equals("sparkTaskB")) {
                        jobResultDTO.setBStartTime(startTime);
                        jobResultDTO.setBEndTime(endTime);
                    }
                    break;
                case BILIBILI_DQC:
                    if (DolphinWorkflowExecutionStatus.isFailure(instanceState)) {
                        jobResultDTO.setDqcResultType("dqc未通过");
                    }
                    if (DolphinWorkflowExecutionStatus.isSuccess(instanceState)) {
                        jobResultDTO.setDqcResultType("dqc已通过");
                    }
                    break;
            }
        }
    }

    @Override
    public ExperimentJobResultDTO transferToWaitingCostJobResultDTO(ExperimentJobProps jobProps,
                                                                     ExperimentJobResult experimentJobResult, Long executionNodeId) {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
        jobResultDTO.setJobId(jobProps.getJobId());
        jobResultDTO.setExecNodeId(executionNodeId);
        jobResultDTO.setJobStatus(ExperimentJobStatus.WAITING_COST);
        final ExperimentType experimentType = jobProps.getExperimentType();
        final String workflowInstanceUrl = experimentJobResult.getWorkflowInstanceUrl();
        fillJobResultByDetailUrl(workflowInstanceUrl, jobResultDTO);
        jobResultDTO.setAApplicationId(experimentJobResult.getEngineQueryIdA());

        switch (experimentType) {
            case COMPARATIVE_TASK:
                jobResultDTO.setBApplicationId(experimentJobResult.getEngineQueryIdB());
                break;
        }
        return jobResultDTO;
    }

    protected void logPersist(TaskEvent taskEvent, String logContent) {
        executionLogService.updateLogContent(taskEvent.getEventId(), LogTypeEnum.EVENT, logContent);
    }

}
