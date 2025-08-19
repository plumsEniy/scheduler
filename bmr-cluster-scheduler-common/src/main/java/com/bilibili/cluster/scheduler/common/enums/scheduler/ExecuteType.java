package com.bilibili.cluster.scheduler.common.enums.scheduler;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 操作类型
 * @Date: 2024/5/13 20:43
 * @Author: nizhiqiang
 */

@Getter
@AllArgsConstructor
public enum ExecuteType {
    RECOVER_SUSPENDED_PROCESS("恢复任务，继续执行"),
    PAUSE("暂停"),
    START_FAILURE_TASK_PROCESS("重跑失败任务"),
    ;

    private String desc;
}
