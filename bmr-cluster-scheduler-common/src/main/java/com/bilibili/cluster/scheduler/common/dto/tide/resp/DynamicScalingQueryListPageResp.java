package com.bilibili.cluster.scheduler.common.dto.tide.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.PageInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class DynamicScalingQueryListPageResp extends BaseMsgResp {

    private PageInfo<DynamicScalingConfDTO> obj;

}
