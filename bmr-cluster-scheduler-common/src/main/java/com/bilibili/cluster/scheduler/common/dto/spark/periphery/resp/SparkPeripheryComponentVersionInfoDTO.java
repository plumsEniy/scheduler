package com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp;

import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import lombok.Data;

@Data
public class SparkPeripheryComponentVersionInfoDTO {

    private String jobId;

    private String jobName;

    private SparkPeripheryComponent component;

    private String targetVersion;

    private boolean isLocked;
}
