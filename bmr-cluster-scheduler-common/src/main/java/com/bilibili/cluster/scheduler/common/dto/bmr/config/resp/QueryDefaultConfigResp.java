package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigVersionEntity;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description: 查询默认配置包
 * @Date: 2024/12/4 16:53
 * @Author: nizhiqiang
 */

@Data
public class QueryDefaultConfigResp extends BaseMsgResp {
    ConfigVersionEntity obj;
}