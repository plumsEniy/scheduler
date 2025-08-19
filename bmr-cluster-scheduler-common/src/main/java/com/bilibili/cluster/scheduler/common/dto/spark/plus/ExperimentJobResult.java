package com.bilibili.cluster.scheduler.common.dto.spark.plus;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.Map;

@Data
public class ExperimentJobResult {

    @Alias(value = "experimentid")
    private String experimentId;

    private String jobId;

    private String businessTime;

    private String cTime;

    @Alias(value = "confa")
    private String confA;

    @Alias(value = "confb")
    private String confB;

    private String description;

    private String instanceExecutedId;

    private String user;



    @Alias(value = "workflowname")
    private String workflowName;

    @Alias(value = "platforma")
    private String platformA;

    @Alias(value = "platformb")
    private String platformB;

    private String metrics;

    private String details;



    private String targetTableA;

    private String targetTableB;

    private String executionStatus;

    private String dqcDiffColumns;

    private String dqcNonDiffColumns;

    private String dqcDetailTable;

    private String failureNode;


    private String workflowInstanceUrl;

    private Map<String, Double> cpu;

    private Map<String, Double> memory;

    private Map<String, Double> duration;

    private String engineQueryIdA;

    private String engineQueryIdB;

}
