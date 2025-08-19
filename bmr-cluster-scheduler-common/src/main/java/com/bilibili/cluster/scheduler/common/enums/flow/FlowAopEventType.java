package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 工作流aop的event事件
 * @Date: 2025/5/12 16:39
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum FlowAopEventType {
    INCIDENT("变更通知"),

    REFRESH_RESOURCE("资源管理系统刷新"),

    JOB_FAIL_NOTIFY("任务失败通知"),
    ;

    @Getter
    private String desc;
}
