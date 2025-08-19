package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QuerySparkJobPageResp extends BaseMsgResp {

    private PageInfo<SparkJobInfoDTO> obj;

}
