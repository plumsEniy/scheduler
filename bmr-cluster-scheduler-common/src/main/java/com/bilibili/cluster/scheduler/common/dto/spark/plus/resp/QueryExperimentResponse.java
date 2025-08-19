package com.bilibili.cluster.scheduler.common.dto.spark.plus.resp;

import com.bilibili.cluster.scheduler.common.dto.spark.plus.ExperimentJobResult;
import lombok.Data;

import java.util.List;

@Data
public class QueryExperimentResponse extends BaseDolphinPlusResp {

    private List<List<ExperimentJobResult>> data;

}
