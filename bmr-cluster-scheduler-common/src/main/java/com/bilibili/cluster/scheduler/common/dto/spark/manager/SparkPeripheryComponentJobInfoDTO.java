package com.bilibili.cluster.scheduler.common.dto.spark.manager;

import lombok.Data;

@Data
public class SparkPeripheryComponentJobInfoDTO {

    /**
     * 自增主键ID
     */
    private Long id;

    private String jobName;

    private String jobId;

    private String targetVersion;

    private String runtimeVersion;

    private boolean lockVersion;

    private String jobType;

    private String jobDescribe;

    private String jobOwner;

    private String jobSla;

    private String jobDepartment;

    private String jobWorkSpace;

    private String traceId;

    private String sqlStatement;

    private String executeUser;

    // 工作流调度名称
    private String flowName;

    private String jobUi;

    private String taskType;

    // 阶段标签
    private String label;

}
