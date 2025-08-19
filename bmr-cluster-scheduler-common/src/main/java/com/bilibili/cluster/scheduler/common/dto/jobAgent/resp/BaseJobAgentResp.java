package com.bilibili.cluster.scheduler.common.dto.jobAgent.resp;

import lombok.Data;

/**
 * @description: jobagent结果
 * @Date: 2024/5/16 10:31
 * @Author: nizhiqiang
 */

@Data
public class BaseJobAgentResp {
    private boolean success;
    private int code;
    private String message;
}
