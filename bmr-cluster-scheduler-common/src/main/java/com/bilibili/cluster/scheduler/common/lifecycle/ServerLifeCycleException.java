package com.bilibili.cluster.scheduler.common.lifecycle;

public class ServerLifeCycleException extends Exception {

    public ServerLifeCycleException(String message) {
        super(message);
    }

    public ServerLifeCycleException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
