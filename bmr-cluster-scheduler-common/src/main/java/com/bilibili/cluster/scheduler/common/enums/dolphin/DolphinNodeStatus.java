package com.bilibili.cluster.scheduler.common.enums.dolphin;

/**
 * @description:
 * @Date: 2024/5/16 10:58
 * @Author: nizhiqiang
 */
public enum DolphinNodeStatus {
    READY,
    RUNNING,
    SUCCESS,
    FAIL,
    ;

    private String desc;

    public boolean isFinish() {
        switch (this) {
            case SUCCESS:
            case FAIL:
                return true;
            default:
                return false;
        }
    }
}
