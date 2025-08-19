package com.bilibili.cluster.scheduler.common.dto.spark.periphery.req;

import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.VersionLockState;
import lombok.Data;

@Data
public class SparkPeripheryComponentVersionUpdateReq {

    private String jobId;

    private SparkPeripheryComponent component;

    private String targetVersion;

    private VersionLockState lockState = VersionLockState.REQUIRE_UNLOCK;

}
