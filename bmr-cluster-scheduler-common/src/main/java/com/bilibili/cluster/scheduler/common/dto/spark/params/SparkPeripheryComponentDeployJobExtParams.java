package com.bilibili.cluster.scheduler.common.dto.spark.params;

import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import lombok.Data;


@Data
public class SparkPeripheryComponentDeployJobExtParams extends BaseNodeParams {

    private String jobId;

    private String jobName;

    private SparkPeripheryComponent peripheryComponent;

    private String originalVersion;

    private String targetVersion;

    private boolean isLocked;

}
