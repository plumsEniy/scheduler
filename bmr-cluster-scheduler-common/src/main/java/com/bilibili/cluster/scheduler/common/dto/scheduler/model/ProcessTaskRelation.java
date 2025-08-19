package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.Map;

/**
 * @description: 任务关系
 * @Date: 2024/5/13 19:28
 * @Author: nizhiqiang
 */

@Data
public class ProcessTaskRelation {

    private String id;
    private String name;
    private String processDefinitionVersion;
    private String projectCode;
    private String processDefinitionCode;
    private String preTaskCode;
    private String preTaskVersion;
    private String postTaskCode;
    private String postTaskVersion;
    private String conditionType;
    private Map<String, String> conditionParams;
    private String createTime;
    private String upStringTime;
    private String operator;
    private String operateTime;
}
