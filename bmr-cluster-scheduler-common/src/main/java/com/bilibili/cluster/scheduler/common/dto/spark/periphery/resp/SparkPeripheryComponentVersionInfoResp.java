package com.bilibili.cluster.scheduler.common.dto.spark.periphery.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class SparkPeripheryComponentVersionInfoResp extends BaseMsgResp {

    private SparkPeripheryComponentVersionInfoDTO obj;

}
