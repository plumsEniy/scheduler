package com.bilibili.cluster.scheduler.common.exception;

/**
 * @description: requester工具类的异常
 * @Date: 2024/3/6 11:48
 * @Author: nizhiqiang
 */
public class RequesterException extends RuntimeException {

    private static final long serialVersionUID = 6536078088056105942L;

    public RequesterException(String message) {
        super(message);
    }

    public RequesterException(Throwable cause) {
        super(cause);
    }

    public RequesterException(String message, Throwable cause) {
        super(message, cause);
    }
}