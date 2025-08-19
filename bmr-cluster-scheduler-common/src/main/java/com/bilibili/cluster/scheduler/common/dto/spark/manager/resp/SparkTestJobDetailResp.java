package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkTestJobDetailDTO;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class SparkTestJobDetailResp extends BaseMsgResp {

    private SparkTestJobDetailDTO obj;

}
