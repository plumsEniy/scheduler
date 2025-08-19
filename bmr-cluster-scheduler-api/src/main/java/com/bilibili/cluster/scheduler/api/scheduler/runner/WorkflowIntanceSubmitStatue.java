package com.bilibili.cluster.scheduler.api.scheduler.runner;

public enum WorkflowIntanceSubmitStatue {
    /**
     * Submit success
     */
    SUCCESS,
    /**
     * Submit failed, this status should be retry
     */
    FAILED,
    /**
     * Duplicated submitted, this status should never occur.
     */
    DUPLICATED_SUBMITTED;
}
