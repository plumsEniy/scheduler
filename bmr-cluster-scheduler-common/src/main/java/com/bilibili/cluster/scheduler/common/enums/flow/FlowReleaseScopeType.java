package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.Getter;

/**
 * @description: 发布类型
 * @Date: 2024/5/9 17:04
 * @Author: nizhiqiang
 */
public enum FlowReleaseScopeType {

    FULL_RELEASE("全量发布"),
    GRAY_RELEASE("灰度发布"),
    YARN_QUEUE("yarn队列"),
    HA_TRANSFER("主从切换"),
    NONE_RELEASE("非发布类型"),

    ADD_MONITOR_OBJECT("增量添加监控对象"),
    REMOVE_MONITOR_OBJECT("增量移除监控对象"),
    ;

    @Getter
    String desc;

    FlowReleaseScopeType(String desc) {
        this.desc = desc;
    }
}
