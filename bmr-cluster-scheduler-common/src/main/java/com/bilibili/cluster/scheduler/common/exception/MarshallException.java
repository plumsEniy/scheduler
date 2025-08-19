package com.bilibili.cluster.scheduler.common.exception;

/**
 * @description:
 * @Date: 2024/3/6 11:55
 * @Author: nizhiqiang
 */
public class MarshallException extends RuntimeException {

    public MarshallException(String message) {
        super(message);
    }

    public MarshallException(Throwable t) {
        super(t);
    }

    public MarshallException(String message, Throwable t) {
        super(message, t);
    }
}