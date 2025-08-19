package com.bilibili.cluster.scheduler.common.dto.bmr.config.resp;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigData;
import com.bilibili.cluster.scheduler.common.response.BaseMsgResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/6/7 15:06
 * @Author: nizhiqiang
 */

@Data
public class QueryConfigFileResp extends BaseMsgResp {
    private ConfigData obj;
}
