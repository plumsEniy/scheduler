package com.bilibili.cluster.scheduler.common.dto.yarn.resp;

import com.bilibili.cluster.scheduler.common.dto.yarn.RMInfoObj;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryRMInfoResp extends BaseMsgResp {

    private RMInfoObj obj;

}
