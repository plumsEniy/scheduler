package com.bilibili.cluster.scheduler.common.dto.flow.req.spark;


import lombok.Data;

import javax.validation.constraints.Positive;

@Data
public class QuerySparkDeployFlowPageReq {

    /**
     * spark 大版本
     */
    private String majorSparkVersion;

    /**
     * spark 目标版本
     */
    private String targetSparkVersion;

    /**
     * 申请｜操作人
     */
    private String opUser;

    private String deployType;

    private String flowStatus;


    private String minStartTime;

    private String maxStartTime;

    @Positive(message = "page num is illegal")
    private int pageNum = 1;

    @Positive(message = "page size is illegal")
    private int pageSize = 10;

}
