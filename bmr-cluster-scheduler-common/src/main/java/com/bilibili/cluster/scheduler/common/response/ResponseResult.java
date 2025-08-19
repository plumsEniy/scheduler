package com.bilibili.cluster.scheduler.common.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: liuguohui
 * description:
 */

@Setter
@Getter
@Builder
public class ResponseResult {

    private Integer code;

    private String msg;

    private Object obj;

    public static ResponseResult getSuccess(Object obj) {
        return ResponseResult.builder().code(0).msg("操作成功").obj(obj).build();
    }

    public static ResponseResult getSuccess() {
        return ResponseResult.builder().code(0).msg("操作成功").build();
    }

    public static ResponseResult getError() {
        return ResponseResult.builder().code(-501).msg("服务端异常").build();
    }

    public static ResponseResult getError(String errorMessage) {
        return ResponseResult.builder().code(-500).msg(errorMessage).build();
    }

    public static ResponseResult getError(int code, String errorMessage) {
        return ResponseResult.builder().code(code).msg(errorMessage).build();
    }

    public static ResponseResult getError(String msg, Object... args) {
        return ResponseResult.builder().code(-500).msg(String.format(msg, args)).build();
    }

    public static ResponseResult getError(String errorMessage, int code) {
        return ResponseResult.builder().code(code).msg(errorMessage).build();
    }

    public static ResponseResult unLogin() {
        return ResponseResult.builder().code(403).msg("User not logged in").build();
    }


}
