package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.RmsHostInfo;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/7/24 11:20
 * @Author: nizhiqiang
 */

@Data
public class QueryHostRmsResp extends BaseMsgResp {
    private RmsHostInfo obj;
}
