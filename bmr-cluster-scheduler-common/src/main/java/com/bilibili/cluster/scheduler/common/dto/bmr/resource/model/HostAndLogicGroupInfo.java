package com.bilibili.cluster.scheduler.common.dto.bmr.resource.model;

import lombok.Data;

/**
 * @description:
 * @Date: 2024/5/15 15:30
 * @Author: nizhiqiang
 */

@Data
public class HostAndLogicGroupInfo {

    private static final long serialVersionUID = 1L;

    private Long clusterId;
    private Long logicGroupId;
    private String logicGroupName;

    private Long id;

    private String rack;

    private String hostName;

    private String ip;

    private String hostState;

    private String diskInfo;

    private String memoryInfo;

    private String cpuInfo;

    private String department;

    private String creator;

    private String updater;

    private Integer numSsd;

    private Integer numSata;

    private Integer numCpuCore;

    private Integer nvmeTotal;

    private String initState;

    private String jobAgentState;

    private Integer jobAgentOrder;

    private String schedInstanceId;

    private String initProjectId;

    private String bs;

    private Integer bmrFlag;

    private String suit;

}
