package com.bilibili.cluster.scheduler.api.event.dolphinScheduler;

public enum DolphinExecState {

    NO_LOCK_ACQUIRED,

    HOLD_LOCK_AND_EXEC,

    LOCK_ACQUIRED_ERROR,
    ;
}
