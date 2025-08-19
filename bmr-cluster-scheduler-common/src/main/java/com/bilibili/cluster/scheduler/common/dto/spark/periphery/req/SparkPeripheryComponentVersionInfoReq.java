package com.bilibili.cluster.scheduler.common.dto.spark.periphery.req;

import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SparkPeripheryComponentVersionInfoReq {

    private SparkPeripheryComponent component;

    private String jobId;

}
