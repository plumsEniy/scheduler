package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description:
 * @Date: 2024/5/13 19:29
 * @Author: nizhiqiang
 */

@Data
public class TaskDefinition {
    private String id;
    private String code;
    private String name;
    private String version;
    private String description;
    private String projectCode;
    private String userId;
    private String taskType;
    private TaskParams taskParams;
    private List<TaskParamList> taskParamList;
    //    private TaskParamMap taskParamMap;
    private String flag;
    private String taskPriority;
    private String userName;
    private String projectName;
    private String workerGroup;
    private String environmentCode;
    private String failRetryTimes;
    private String failRetryStringerval;
    private String timeoutFlag;
    private String timeoutNotifyStrategy;
    private String timeout;
    private String delayTime;
    private String resourceIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String modifyBy;
    private String taskGroupId;
    private String taskGroupPriority;
    private String cpuQuota;
    private String memoryMax;
    private String taskExecuteType;
    private String operator;
    private LocalDateTime operateTime;
    private String dependence;

    @Data
    public static class TaskParams {

        private List<LocalParams> localParams;
        private String rawScript;
        private List<String> resourceList;
    }

    class LocalParams {

        private String prop;
        private String direct;
        private String type;
        private String value;
    }

    class TaskParamList {

        private String prop;
        private String direct;
        private String type;
        private String value;


    }
}
