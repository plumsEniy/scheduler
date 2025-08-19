package com.bilibili.cluster.scheduler.common.dto.spark.plus.resp;

import com.bilibili.cluster.scheduler.common.dto.spark.plus.CreateExperimentData;
import lombok.Data;

import java.util.List;

@Data
public class CreateExperimentResponse extends BaseDolphinPlusResp {

    private List<CreateExperimentData> data;

}
