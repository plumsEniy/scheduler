package com.bilibili.cluster.scheduler.common.dto.scheduler.resp;

import lombok.Data;

/**
 * @description: jobagentbase resp
 * @Date: 2024/5/13 17:59
 * @Author: nizhiqiang
 */

@Data
public class BaseDolphinSchedulerResp {
    private int code;
    private String msg;
    private Object data;
    private boolean failed;
    private boolean success;
}
