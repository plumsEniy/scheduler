package com.bilibili.cluster.scheduler.common.dto.spark.params;

import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import lombok.Data;

@Data
public class SparkDeployJobExtParams extends BaseNodeParams {

    private String jobName;

    private String oldSparkVersion;

    private String targetSparkVersion;

    private String jobId;

    private boolean lockSparkVersion;
}
