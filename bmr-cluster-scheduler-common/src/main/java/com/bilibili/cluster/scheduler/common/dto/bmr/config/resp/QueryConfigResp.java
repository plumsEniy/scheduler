package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询配置
 * @Date: 2024/5/15 16:15
 * @Author: nizhiqiang
 */
@Data
public class QueryConfigResp extends BaseMsgResp {
    private ConfigDetailData obj;

}
