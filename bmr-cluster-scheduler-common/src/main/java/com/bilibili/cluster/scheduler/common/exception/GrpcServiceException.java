package com.bilibili.cluster.scheduler.common.exception;

import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.Status;
import lombok.Data;

/**
 * @description: grpc服务的异常类
 * @Date: 2023/7/17 16:31
 * @Author: xiexieliangjie
 */

@Data
public class GrpcServiceException extends RuntimeException {

    public int code = Status.GRPC_EXCEPTION.getCode();
    public String message;
    public String serviceName = Constants.EMPTY_STRING;

    public GrpcServiceException() {
    }

    public GrpcServiceException(String message) {
        super(message);
        this.message = message;
    }

    public GrpcServiceException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    public GrpcServiceException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
