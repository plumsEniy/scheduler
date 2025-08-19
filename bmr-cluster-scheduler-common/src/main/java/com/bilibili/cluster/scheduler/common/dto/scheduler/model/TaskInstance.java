package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinWorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/5/13 18:03
 * @Author: nizhiqiang
 */

@Data
public class TaskInstance {

    private String id;
    private String name;
    private DolpTaskType taskType;
    private String processInstanceId;
    private String taskCode;
    private String taskDefinitionVersion;
    private String processInstanceName;
    private String processDefinitionName;
    private String taskGroupPriority;
    private DolphinWorkflowExecutionStatus state;
    private String firstSubmitTime;
    private String submitTime;
    private String startTime;
    private String endTime;
    private String host;
    private String executePath;
    private String logPath;
    private String retryTimes;
    private String alertFlag;
    private String processInstance;
    private String processDefine;
    private String taskDefine;
    private String pid;
    private String appLink;
    private String flag;
    private String dependency;
    private String switchDependency;
    private String duration;
    private String maxRetryTimes;
    private String retryStringerval;
    private String taskInstancePriority;
    private String processInstancePriority;
    private String dependentResult;
    private String workerGroup;
    private String environmentCode;
    private String environmentConfig;
    private String executorId;
    private String varPool;
    private String executorName;
    private String resources;
    private String delayTime;
    private String taskParams;
    private String dryRun;
    private String taskGroupId;
    private String cpuQuota;
    private String memoryMax;
    private String taskExecuteType;
    private String jobAgentResult;
    private boolean taskComplete;
    private boolean dependTask;
    private boolean conditionsTask;
    private boolean switchTask;
    private boolean blockingTask;
    private boolean subProcess;
    private boolean firstRun;
}
