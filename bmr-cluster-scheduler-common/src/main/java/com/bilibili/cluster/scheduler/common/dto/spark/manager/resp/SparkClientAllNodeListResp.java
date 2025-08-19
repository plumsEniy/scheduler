package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkClientNodeInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class SparkClientAllNodeListResp extends BaseMsgResp {

    private List<SparkClientNodeInfo> obj;

}
