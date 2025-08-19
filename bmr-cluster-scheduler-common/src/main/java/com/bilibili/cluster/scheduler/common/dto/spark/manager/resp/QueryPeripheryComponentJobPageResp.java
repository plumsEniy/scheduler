package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkPeripheryComponentJobInfoDTO;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryPeripheryComponentJobPageResp extends BaseMsgResp {

    private PageInfo<SparkPeripheryComponentJobInfoDTO> obj;

}
