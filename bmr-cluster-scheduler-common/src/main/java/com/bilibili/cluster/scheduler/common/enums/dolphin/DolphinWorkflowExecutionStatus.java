package com.bilibili.cluster.scheduler.common.enums.dolphin;

import lombok.Getter;

/**
 * @description: 工作流状态
 * @Date: 2024/5/16 09:24
 * @Author: nizhiqiang
 */
public enum DolphinWorkflowExecutionStatus {

    SUBMITTED_SUCCESS("submit success"),
    RUNNING_EXECUTION("running"),
    READY_PAUSE("ready pause"),
    PAUSE("pause"),
    READY_STOP("ready stop"),
    STOP("stop"),
    FAILURE("failure"),
    SUCCESS("success"),
    DELAY_EXECUTION("delay execution"),
    SERIAL_WAIT("serial wait"),
    READY_BLOCK("ready block"),
    BLOCK("block"),
    ;
    @Getter
    String desc;

    DolphinWorkflowExecutionStatus(String desc) {
        this.desc = desc;
    }

    public static boolean isFailure(DolphinWorkflowExecutionStatus status) {
        return FAILURE.equals(status);
    }

    public static boolean isSuccess(DolphinWorkflowExecutionStatus status) {
        return SUCCESS.equals(status);
    }

    public static boolean isFinish(DolphinWorkflowExecutionStatus status) {
        return FAILURE.equals(status) ||
                SUCCESS.equals(status);
    }

    public static boolean isPause(DolphinWorkflowExecutionStatus status) {
        return PAUSE.equals(status);
    }

    public static boolean isReadyPause(DolphinWorkflowExecutionStatus status) {
        return READY_PAUSE.equals(status);
    }

    public static boolean isPauseOrReadyPause(DolphinWorkflowExecutionStatus status) {
        return isPause(status) || isReadyPause(status);
    }

    public static boolean isRunning(DolphinWorkflowExecutionStatus status) {
        return RUNNING_EXECUTION.equals(status);
    }
}
