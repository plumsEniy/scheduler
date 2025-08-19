package com.bilibili.cluster.scheduler.api.service.experiment;


import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobResultDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import com.bilibili.cluster.scheduler.common.dto.translation.resp.SqlTranslateResult;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;

/**
 * 实验任务相关整合Api
 */
public interface ExperimentJobService {

    void checkTranslateResultTableName(SqlTranslateResult sqlTranslateResult, TaskEvent taskEvent);

    ExperimentJobResultDTO transferToWaitingCostJobResultDTO(ExperimentJobProps jobProps,
                                                             ExperimentJobResult experimentJobResult, Long executionNodeId);

    ExperimentJobResultDTO transferToFailureJobResultDTO(ExperimentJobProps jobProps,
                                                         ExperimentJobResult experimentJobResult, Long executionNodeId);

    ExperimentJobResultDTO transferToSuccessJobResultDTO(ExperimentJobProps jobProps,
                                                         ExperimentJobResult experimentJobResult, long execNodeId);

    void updateSparkCiJobToFailure(ExperimentJobProps jobProps,
                                   ExperimentJobResult experimentJobResult, long execNodeId);

    ExperimentJobResultDTO queryExperimentJobResultByExecNodeId(long execNodeId);

    ExperimentJobResultDTO transferToRunningJobResultDTO(ExperimentJobProps jobProps,
                                                         ExperimentJobResult experimentJobResult, long execNodeId);

}
