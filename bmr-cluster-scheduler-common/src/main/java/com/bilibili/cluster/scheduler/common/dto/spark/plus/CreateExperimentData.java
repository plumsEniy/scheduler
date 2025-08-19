package com.bilibili.cluster.scheduler.common.dto.spark.plus;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.Map;

@Data
public class CreateExperimentData {

    @Alias(value = "experimentid")
    private String experimentId;

    private String user;

    private String description;

    private String jobs;

    @Alias(value = "workflowname")
    private String workflowName;

    @Alias(value = "platforma")
    private String platformA;

    @Alias(value = "platformb")
    private String platformB;

    @Alias(value = "confa")
    private String confA;

    @Alias(value = "confb")
    private String confB;

    /**
     * 支持以下Metric，选取需要的用逗号分隔即可：CPU,MEMORY,DURATION,TABLE_SIZE,BILL
     */
    private String metrics;

    private String status;


    private Map<String, Object> cpu;
    private Map<String, Object> duration;
    private Map<String, Object> memory;

    private String engineQueryIdA;

    private String engineQueryIdB;





}
