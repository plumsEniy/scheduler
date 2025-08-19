package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ComponentHostRelationModel implements Serializable {

    private Long componentId;

    private String componentName;

    private Long clusterId;

    private String hostName;

    private String applicationState;

    private String packageDiskVersion;

    private String packageRuntimeVersion;

    private String configDiskVersion;

    private String configRuntimeVersion;

    private String creator;

    private String updater;

    private LocalDateTime ctime;

    private LocalDateTime mtime;

    private String haState;

    private String reRack;

    private String jobAgentState;

    private Long logicGroupId;

    private String logicGroupName;

    private String ip;

    private String labelName;

    private String hostStatus;

    private String team;

    private String numCpuCore;

    private String cpuInfo;

    private String memoryInfo;

    private String rack;

    private String zone;
}
