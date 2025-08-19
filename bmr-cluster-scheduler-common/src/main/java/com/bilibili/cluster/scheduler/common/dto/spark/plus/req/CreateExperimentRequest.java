package com.bilibili.cluster.scheduler.common.dto.spark.plus.req;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * https://info.bilibili.co/pages/viewpage.action?pageId=848183915
 */
@Data
public class CreateExperimentRequest {

    private String user;

    private String description;

    private String jobs;

    @Alias(value = "workflowname")
    private String workflowName;

    @Alias(value  = "platforma")
    private String platformA;

    @Alias(value  = "platformb")
    private String platformB;

    @Alias(value  = "confa")
    private String confA;

    @Alias(value  = "confb")
    private String confB;

    /**
     * 支持以下Metric，选取需要的用逗号分隔即可：CPU,MEMORY,DURATION,TABLE_SIZE,BILL
     */
    private String metrics;

    private String details = "spark-manager experiment task";

}
