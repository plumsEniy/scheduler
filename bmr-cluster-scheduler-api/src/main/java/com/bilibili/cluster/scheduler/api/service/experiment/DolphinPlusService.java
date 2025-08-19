package com.bilibili.cluster.scheduler.api.service.experiment;


import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.CreateExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.req.QueryExperimentRequest;
import com.bilibili.cluster.scheduler.common.dto.spark.plus.CreateExperimentData;


public interface DolphinPlusService {

    CreateExperimentData createExperimentTask(CreateExperimentRequest request);

    ExperimentJobResult queryExperimentResult(QueryExperimentRequest request);

}
