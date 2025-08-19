package com.bilibili.cluster.scheduler.common.dto.spark.manager;

import lombok.Data;

@Data
public class SparkJobInfoDTO {

    private String jobId;

    private String sqlStatement;

    private SparkJobType jobType;

    private String jobOwner;

    private String jobSla;

    private String targetSparkVersion;

    private String jobName;

    private String defaultRunningParameters;

    /**
     * true is version locked
     */
    private boolean lockSparkVersion;

    private SparkJobLabel label;

}
