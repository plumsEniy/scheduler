package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.List;

/**
 * @description: data
 * @Date: 2024/5/13 18:02
 * @Author: nizhiqiang
 */

@Data
public class SchedInstanceData {

    private String id;
    private String processDefinitionCode;
    private String processDefinitionVersion;
    private String state;
    private String stateHistory;
    private String stateDescList;
    private String recovery;
    private String startTime;
    private String endTime;
    private String runTimes;
    private String name;
    private String host;
    private String processDefinition;
    private String commandType;
    private String commandParam;
    private String taskDependType;
    private String maxTryTimes;
    private String failureStrategy;
    private String warningType;
    private String warningGroupId;
    private String scheduleTime;
    private String commandStartTime;
    private String globalParams;
    private DagData dagData;
    private String executorId;
    private String executorName;
    private String tenantCode;
    private String queue;
    private String isSubProcess;
    private String locations;
    private String historyCmd;
    private String dependenceScheduleTimes;
    private String duration;
    private String processInstancePriority;
    private String workerGroup;
    private String environmentCode;
    private String timeout;
    private String tenantId;
    private String varPool;
    private String nextProcessInstanceId;
    private String dryRun;
    private String restartTime;
    private String executeId;
    private List<TaskInstance> taskInstanceList;
    private boolean complementData;
    private boolean blocked;
    private String cmdTypeIfComplement;

}
