package com.bilibili.cluster.scheduler.api.configuration;

import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MyControllerAdvice {
    protected Logger log = LogManager.getLogger(getClass());

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseResult handleMyException(Exception e) {
        log.error(e);
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException exception = (MethodArgumentNotValidException) e;
            FieldError fieldError = exception.getBindingResult().getFieldError();
            return ResponseResult.builder().code(-500).msg(fieldError.getDefaultMessage()).build();
        }
        return ResponseResult.builder().code(-502).msg("服务端发生异常：" + e.getMessage()).build();
    }

}
