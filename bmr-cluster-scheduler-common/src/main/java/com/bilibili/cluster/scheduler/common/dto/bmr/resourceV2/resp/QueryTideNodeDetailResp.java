package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryTideNodeDetailResp extends BaseMsgResp {

    private TideNodeDetail obj;

}

