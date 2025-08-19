package com.bilibili.cluster.scheduler.common.dto.bmr.resource.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ComponentNodeListObj;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryComponentNodeListResp extends BaseMsgResp {

    private ComponentNodeListObj obj;

}
