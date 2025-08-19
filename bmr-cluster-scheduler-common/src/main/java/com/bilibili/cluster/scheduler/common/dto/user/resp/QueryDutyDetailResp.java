package com.bilibili.cluster.scheduler.common.dto.user.resp;

import com.bilibili.cluster.scheduler.common.response.BaseResp;
import com.bilibili.cluster.scheduler.common.dto.user.DutyDetail;
import lombok.Data;

/**
 * @description: 查询值班日志
 * @Date: 2024/3/19 17:56
 * @Author: nizhiqiang
 */
@Data
public class QueryDutyDetailResp extends BaseResp {
    private DutyDetail data;
}
