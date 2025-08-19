package com.bilibili.cluster.scheduler.common.enums.bmr.flow;

import lombok.AllArgsConstructor;

/**
 * @description:
 * @Date: 2024/5/23 10:56
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum BmrFlowStatus {
    READY("准备中"),

    RUNNING("运行中"),
    FAILED("出错"),
    SUCCESS("成功"),
    ;

    String desc;

}
