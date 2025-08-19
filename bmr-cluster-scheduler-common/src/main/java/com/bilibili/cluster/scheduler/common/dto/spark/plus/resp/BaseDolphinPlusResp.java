package com.bilibili.cluster.scheduler.common.dto.spark.plus.resp;

import lombok.Data;

@Data
public class BaseDolphinPlusResp {

    private int errorCode;

    private int status;

    private String stackTrace;

}
