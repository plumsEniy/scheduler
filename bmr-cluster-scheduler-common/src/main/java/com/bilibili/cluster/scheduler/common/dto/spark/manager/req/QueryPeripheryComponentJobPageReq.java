package com.bilibili.cluster.scheduler.common.dto.spark.manager.req;

import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QueryPeripheryComponentJobPageReq {

    @NotNull(message = "组件类型为空")
    private SparkPeripheryComponent component;

    @Positive(message = "page num is illegal")
    private int pageNum = 1;

    @Positive(message = "page size is illegal")
    private int pageSize = 999999;

    private List<String> jobId;

    private String jobName;

    private String traceId;

    private String jobSla;

    private String jobType;

    private String jobWorkSpace;

    private String flowName;

    private String executeUser;

    private Boolean lockVersion;

    private String targetVersion;

    private String runtimeVersion;

    private String taskType;

    // 排除的任务版本
    private String excludedTargetVersion;

    // 阶段标签
    private String label;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;


    // only used for spark deploy
    private String majorSparkVersion;

    private Boolean isSpark4Version;

}
