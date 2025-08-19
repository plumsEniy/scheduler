package com.bilibili.cluster.scheduler.api.exceptions;

public class WorkflowIntanceHandleException extends Exception {

    public WorkflowIntanceHandleException(String message) {
        super(message);
    }

    public WorkflowIntanceHandleException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
