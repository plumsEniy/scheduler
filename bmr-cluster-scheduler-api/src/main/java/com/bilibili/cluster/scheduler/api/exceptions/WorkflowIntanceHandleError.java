package com.bilibili.cluster.scheduler.api.exceptions;

public class WorkflowIntanceHandleError extends Exception {

    public WorkflowIntanceHandleError(String message) {
        super(message);
    }

    public WorkflowIntanceHandleError(String message, Throwable throwable) {
        super(message, throwable);
    }
}
