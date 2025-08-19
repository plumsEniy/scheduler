
package com.bilibili.cluster.scheduler.api.exceptions;

public class TaskEventHandleException extends Exception {

    public TaskEventHandleException(String message) {
        super(message);
    }

    public TaskEventHandleException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
