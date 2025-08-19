package com.bilibili.cluster.scheduler.api.exceptions;

public class WorkflowInstanceTaskEventHandleException extends Exception {

    public WorkflowInstanceTaskEventHandleException(String message) {
        super(message);
    }

    public WorkflowInstanceTaskEventHandleException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
