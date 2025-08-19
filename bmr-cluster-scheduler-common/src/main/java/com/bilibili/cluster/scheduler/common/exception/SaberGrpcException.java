package com.bilibili.cluster.scheduler.common.exception;

import com.bilibili.cluster.scheduler.common.Constants;

public class SaberGrpcException extends GrpcServiceException {

    {
        this.serviceName = Constants.SABER;
    }

    public SaberGrpcException() {
    }

    public SaberGrpcException(String message) {
        super(message);
    }

    public SaberGrpcException(Throwable cause) {
        super(cause);
    }

    public SaberGrpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
