package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

@Data
public class QueryIsHolidayResp extends BaseMsgResp {

    private Boolean obj;

}
