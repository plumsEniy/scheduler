package com.bilibili.cluster.scheduler.api.event.dolphinScheduler;


public enum DolphinPreAlignState {

    NO_LOCK_ACQUIRED,

    HOLD_LOCK_AND_WAIT,

    HOLD_LOCK_AND_ALIGNED,

    LOCK_ACQUIRED_ERROR,

    ;
}
