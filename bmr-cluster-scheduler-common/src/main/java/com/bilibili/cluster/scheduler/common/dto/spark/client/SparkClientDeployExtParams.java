package com.bilibili.cluster.scheduler.common.dto.spark.client;

import lombok.Data;

import java.util.List;

@Data
public class SparkClientDeployExtParams {

    private List<Long> packIdList;

    private SparkClientDeployType packDeployType;

    private List<SparkClientPackInfo> packInfoList;

}
