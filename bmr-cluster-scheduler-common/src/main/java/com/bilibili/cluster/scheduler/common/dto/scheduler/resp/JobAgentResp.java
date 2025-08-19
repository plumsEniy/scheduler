package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;

import com.bilibili.cluster.scheduler.common.dto.scheduler.model.JData;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description: jobagent的响应
 * @Date: 2024/5/13 17:57
 * @Author: nizhiqiang
 */

@Data
public class JobAgentResp extends BaseResp {
    private boolean success;

    private JData data;

}
