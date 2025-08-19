package com.bilibili.cluster.scheduler.common.dto.spark.manager.req;

import lombok.Data;

import java.util.List;

@Data
public class QuerySparkJobPageReq {

    private String executeUser;

    private String flowName;

    private List<String> jobId;

    private String jobName;

    private String jobSla;

    private String jobType;

    private String jobWorkSpace;

    private Boolean lockSparkVersion;

    private int pageNum;

    private int pageSize;

    private String runtimeSparkVersion;

    private String targetSparkVersion;

    private String traceId;

}
