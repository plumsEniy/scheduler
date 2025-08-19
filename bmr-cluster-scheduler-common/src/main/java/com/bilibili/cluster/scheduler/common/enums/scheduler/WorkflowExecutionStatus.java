 package com.bilibili.cluster.scheduler.common.enums.scheduler;

import lombok.Getter;

public enum WorkflowExecutionStatus {
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

    WorkflowExecutionStatus(String desc) {
        this.desc = desc;
    }

    public static boolean isFailure(String status) {
        return FAILURE.name().equalsIgnoreCase(status);
    }

    public static boolean isSuccess(String status) {
        return SUCCESS.name().equalsIgnoreCase(status);
    }

    public static boolean isFinish(String state) {
        return FAILURE.name().equalsIgnoreCase(state) ||
                SUCCESS.name().equalsIgnoreCase(state);
    }

    public static boolean isPause(String state) {
        return PAUSE.name().equalsIgnoreCase(state);
    }

    public static boolean isReadyPause(String state) {
        return READY_PAUSE.name().equalsIgnoreCase(state);
    }

    public static boolean isPauseOrReadyPause(String state) {
        return isPause(state) || isReadyPause(state);
    }

    public static boolean isRunning(String state) {
        return RUNNING_EXECUTION.name().equalsIgnoreCase(state);
    }
}









