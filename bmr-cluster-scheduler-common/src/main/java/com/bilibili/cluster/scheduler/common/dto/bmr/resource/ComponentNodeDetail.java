package com.bilibili.cluster.scheduler.common.dto.bmr.resource;

import lombok.Data;

/**
 * @description: 组件节点信息
 * @Date: 2024/5/13 14:50
 * @Author: nizhiqiang
 */
@Data
public class ComponentNodeDetail {
    private String ctime;
    private String mtime;
    private String creator;
    private String updater;
    private boolean deleted;
    private long id;
    private String componentName;
    private long componentId;
    private String hostName;
    private String applicationState;
    private String packageDiskVersion;
    private String packageRuntimeVersion;
    private String configDiskVersion;
    private String configRuntimeVersion;
    private String logicGroupName;
    private String jobAgentState;
    private String yarnNodeLabel;
    private String haState;
    private String ip;

    private String rack;

    /**
     * 目前只有nnproxy有这个字段
     */
    private String dns;

}