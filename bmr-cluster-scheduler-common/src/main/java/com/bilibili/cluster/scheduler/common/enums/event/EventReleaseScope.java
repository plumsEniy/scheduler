package com.bilibili.cluster.scheduler.common.enums.event;

import lombok.AllArgsConstructor;

/**
 * @description: 节点范围
 * @Date: 2024/5/14 15:53
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum EventReleaseScope {

    EVENT("事件级别"),
    GLOBAL("全部范围"),
    BATCH("批次级别"),
    STAGE("阶段级别"),
    ;

    private String desc;
}
