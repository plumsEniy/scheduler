package com.bilibili.cluster.scheduler.common.exception;

public class DolphinPlusInvokerException extends RuntimeException {

    public DolphinPlusInvokerException(String message) {
        super(message);
    }

    public DolphinPlusInvokerException(Throwable t) {
        super(t);
    }

    public DolphinPlusInvokerException(String message, Throwable t) {
        super(message, t);
    }

}
