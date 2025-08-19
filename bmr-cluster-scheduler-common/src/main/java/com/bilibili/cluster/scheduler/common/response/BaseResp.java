package com.bilibili.cluster.scheduler.common.response;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

/**
 * @description: 基本的response类
 * @Date: 2023/7/21 15:46
 * @Author: xiexieliangjie
 */
@Data
@ToString(callSuper = true)
public class BaseResp implements Serializable {
    private int code;
    private String message;
}
