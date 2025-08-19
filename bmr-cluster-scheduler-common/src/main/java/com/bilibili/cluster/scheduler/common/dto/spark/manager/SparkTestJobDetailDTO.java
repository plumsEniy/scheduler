package com.bilibili.cluster.scheduler.common.dto.spark.manager;

import lombok.Data;

@Data
public class SparkTestJobDetailDTO {

    private String jobId;

    private String jobDetails;

    private String creator;

    private String name;

    private String defaultRunningParameters;

}
