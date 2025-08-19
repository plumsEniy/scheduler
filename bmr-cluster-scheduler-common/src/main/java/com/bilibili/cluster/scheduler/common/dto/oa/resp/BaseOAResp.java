package com.bilibili.cluster.scheduler.common.dto.oa.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: oa基础回复
 * @Date: 2024/3/6 11:03
 * @Author: nizhiqiang
 */

@Data
public abstract class BaseOAResp implements Serializable {
    private boolean success;
    private int code;
    private String message;
}
