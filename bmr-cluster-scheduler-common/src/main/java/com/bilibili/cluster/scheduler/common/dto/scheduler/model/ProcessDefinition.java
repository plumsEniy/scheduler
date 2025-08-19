package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2024/5/13 18:04
 * @Author: nizhiqiang
 */

@Data
public class ProcessDefinition {

    private String id;
    private String code;
    private String name;
    private String version;
    private String releaseState;
    private String projectCode;
    private String description;
    private String globalParams;
    private List<GlobalParam> globalParamList;
    private Map<String, String> globalParamMap;
    private String createTime;
    private String updateTime;
    private String flag;
    private String userId;
    private String userName;
    private String projectName;
    private String locations;
    private String scheduleReleaseState;
    private String timeout;
    private String tenantId;
    private String tenantCode;
    private String modifyBy;
    private String warningGroupId;
    private String executionType;

}
