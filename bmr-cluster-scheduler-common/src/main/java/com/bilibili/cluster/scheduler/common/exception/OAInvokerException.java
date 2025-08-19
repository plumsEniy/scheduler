package com.bilibili.cluster.scheduler.common.exception;

public class OAInvokerException extends RuntimeException {

    public OAInvokerException(String message) {
        super(message);
    }

    public OAInvokerException(Throwable cause) {
        super(cause);
    }

    public OAInvokerException(String message, Throwable cause) {
        super(message, cause);
    }
}
