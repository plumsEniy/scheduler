package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/5/13 19:23
 * @Author: nizhiqiang
 */

@Data
public class TaskParaObj {

    private List<Object> localParams;
    private String rawScript;
    private List<Object> resourceList;
    private String conditionResult;
    private String dependence;
    private String switchResult;
    private String waitStartTimeout;

}
