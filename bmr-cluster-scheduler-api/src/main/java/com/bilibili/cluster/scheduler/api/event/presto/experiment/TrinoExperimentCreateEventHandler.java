package com.bilibili.cluster.scheduler.api.event.presto.experiment;


import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractBranchedTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.api.service.experiment.DolphinPlusService;
import com.bilibili.cluster.scheduler.api.service.experiment.ExperimentJobService;
import com.bilibili.cluster.scheduler.api.service.translation.SqlTranslateService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobStatus;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.CreateExperimentData;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.SparkManagerJob;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.CreateExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.translation.req.SqlTranslateConf;
import com.bilibili.cluster.scheduler.common.dto.translation.req.SqlTranslateReq;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResult;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
public class TrinoExperimentCreateEventHandler extends AbstractBranchedTaskEventHandler {

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    SqlTranslateService sqlTranslateService;

    @Resource
    DolphinPlusService dolphinPlusService;

    @Resource
    ExperimentJobService experimentJobService;

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

    /**
     * 处理普通可执行节点
     * @param taskEvent
     * @return
     * @throws Exception
     */
    public boolean executeNormalNodeTaskEvent(TaskEvent taskEvent) throws Exception {
        final ExperimentJobResultDTO jobResultDTO = new ExperimentJobResultDTO();
        try {
            Long executionNodeId = taskEvent.getExecutionNodeId();
            final ExperimentJobProps jobProps = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, ExperimentJobProps.class);
            jobProps.setNodeId(executionNodeId);
            logPersist(taskEvent, "实验任务额外参数为: " + JSONUtil.toJsonStr(jobProps));
            jobResultDTO.setCiInstanceId(jobProps.getCiInstanceId());
            jobResultDTO.setExecNodeId(executionNodeId);
            final String jobId = jobProps.getJobId();
            jobResultDTO.setJobId(jobId);

            final ExperimentJobType jobType = jobProps.getJobType();
            SparkJobInfoDTO sparkJobInfoDTO = sparkManagerService.querySparkJobInfo(jobId,
                    jobType, jobProps.getTestSetVersionId());


            final String sqlCode = sparkJobInfoDTO.getSqlStatement();
            jobProps.setJobName(sparkJobInfoDTO.getJobName());
            jobProps.setSqlCode(sqlCode);
            executionNodePropsService.saveNodeProp(executionNodeId, jobProps);

            final SparkManagerJob job = new SparkManagerJob();
            job.setJobId(jobId);
            String platformA = jobProps.getPlatformA();
            job.setBusinessTime(LocalDateFormatterUtils.getNowMilliFmt());

            CreateExperimentRequest createExperimentRequest = new CreateExperimentRequest();
            createExperimentRequest.setUser(jobProps.getOpUser());
            createExperimentRequest.setDescription("trino-manager实验任务");
            createExperimentRequest.setPlatformA(platformA);
            createExperimentRequest.setConfA(jobProps.getConfA());
            createExperimentRequest.setMetrics(jobProps.getMetrics());

            String platformB = jobProps.getPlatformB();
            createExperimentRequest.setPlatformB(platformB);
            final ExperimentType experimentType = jobProps.getExperimentType();

            SqlTranslateReq sqlTranslateReq = new SqlTranslateReq();
            sqlTranslateReq.setQuery(sqlCode);
            final SqlTranslateConf conf = SqlTranslateConf.getTrinoConf();
            sqlTranslateReq.setConf(conf);
            logPersist(taskEvent, "经sql转化操作,请求参数为: " + JSONUtil.toJsonStr(sqlTranslateReq));

            SqlTranslateResult sqlTranslateResult = sqlTranslateService.getSqlTranslateResult(sqlTranslateReq);
            experimentJobService.checkTranslateResultTableName(sqlTranslateResult, taskEvent);

            StringBuilder sourceSqlBuilder = new StringBuilder();
            String sourceEngineTargetTable = sqlTranslateResult.getSourceEngineTargetTable();
            sourceSqlBuilder.append(sqlTranslateResult.getSourceEngineDropSql())
                    .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);
            final String sourceEngineCreateSql = sqlTranslateResult.getSourceEngineCreateSql();
            if (!StringUtils.isBlank(sourceEngineCreateSql)) {
                sourceSqlBuilder.append(sourceEngineCreateSql)
                        .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);
            }
            sourceSqlBuilder.append(sqlTranslateResult.getSourceEngineSql())
                    .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);

            StringBuilder targetSqlBuilder = new StringBuilder();
            String targetEngineTargetTable = sqlTranslateResult.getTargetEngineTargetTable();
            targetSqlBuilder.append(sqlTranslateResult.getTargetEngineDropSql())
                    .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);
            final String targetEngineCreateSql = sqlTranslateResult.getTargetEngineCreateSql();
            if (!StringUtils.isBlank(targetEngineCreateSql)) {
                targetSqlBuilder.append(targetEngineCreateSql)
                        .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);
            }
            targetSqlBuilder.append(sqlTranslateResult.getTargetEngineSql())
                    .append(Constants.SEMI_COLON).append(Constants.NEW_LINE);

            switch (experimentType) {
                case PERFORMANCE_TEST:
                    createExperimentRequest.setWorkflowName("trino-manager单路实验任务");
                    if (platformA.contains("_a")) {
                        job.setCodeA(sourceSqlBuilder.toString());
                        job.setTargetTableA(sourceEngineTargetTable);
                    } else {
                        job.setCodeA(targetSqlBuilder.toString());
                        job.setTargetTableA(targetEngineTargetTable);
                    }
                    break;
                case COMPARATIVE_TASK:
                    createExperimentRequest.setWorkflowName("trino-manager双路实验任务");
                    if (platformA.contains("_a")) {
                        job.setCodeA(sourceSqlBuilder.toString());
                        job.setTargetTableA(sourceEngineTargetTable);
                        job.setCodeB(targetSqlBuilder.toString());
                        job.setTargetTableB(targetEngineTargetTable);
                    } else {
                        job.setCodeA(targetSqlBuilder.toString());
                        job.setTargetTableA(targetEngineTargetTable);
                        job.setCodeB(sourceSqlBuilder.toString());
                        job.setTargetTableB(sourceEngineTargetTable);
                    }
                    createExperimentRequest.setConfB(jobProps.getConfB());
                    break;
                default:
                    throw new RuntimeException("un support experimentType of:" + experimentType);
            }

            String jobs = JSONUtil.toJsonStr(Arrays.asList(job));
            createExperimentRequest.setJobs(jobs);
            logPersist(taskEvent, "创建实验任务请求参数为: " + JSONUtil.toJsonStr(createExperimentRequest));

            // create experiment job
            CreateExperimentData experimentTask = dolphinPlusService.createExperimentTask(createExperimentRequest);
            final String experimentId = experimentTask.getExperimentId();
            logPersist(taskEvent, "创建实验任务成功,实验id为: " + experimentId);
            jobProps.setExperimentId(experimentId);
            jobProps.setStartTs(System.currentTimeMillis());

            // update experiment id to job props
            executionNodePropsService.saveNodeProp(executionNodeId, jobProps);
            // update spark manager ci job to running
            jobResultDTO.setJobStatus(ExperimentJobStatus.RUNNING);
            final LocalDateTime now = LocalDateTime.now();
            jobResultDTO.setAStartTime(now);
            if (experimentType == ExperimentType.COMPARATIVE_TASK) {
                jobResultDTO.setBStartTime(now);
            }

            sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            logPersist(taskEvent, "创建实验任务失败,原因：" + e.getMessage());
            jobResultDTO.setJobStatus(ExperimentJobStatus.FAIL);
            sparkManagerService.updateSparkCiJobInfo(jobResultDTO);
            return false;
        }
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.TRINO_EXPERIMENT_CREATE_EXEC_EVENT;
    }

}
