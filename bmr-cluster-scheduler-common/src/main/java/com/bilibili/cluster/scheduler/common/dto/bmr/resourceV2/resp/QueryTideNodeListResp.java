package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

import java.util.List;

@Data
public class QueryTideNodeListResp extends BaseMsgResp {

    private List<TideNodeDetail> obj;

}
