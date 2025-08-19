package com.bilibili.cluster.scheduler.common.dto.spark.manager.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class QueryRelationComponentAllJobListResp extends BaseMsgResp {

    private List<String> obj;

}
