package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req;

import lombok.Data;

import java.util.List;

@Data
public class QueryComponentHostPageReq {
    /**
     * 主机名列表
     */
    private List<String> hostNameList;
    /**
     * 组件id
     */
    private Long componentId;
    /**
     * 集群id
     */
    private Long clusterId;
    /**
     * 服务运行状态
     */
    private String applicationState;
    /**
     * 磁盘上安装包
     */
    private String packageDiskVersion;
    /**
     * 当前运行的安装包
     */
    private String packageRuntimeVersion;
    /**
     * 磁盘上配置包版本
     */
    private String configDiskVersion;
    /**
     * 当前运行配置包版本
     */
    private String configRuntimeVersion;

    /**
     * label name
     */
    private String labelName;

    /**
     * job agent 状态
     */
    private String jobAgentState;
    /**
     * 节点组名
     */
    private String groupName;

    /**
     * 上下线状态
     */
    private String hostStatus;

    /**
     * 潮汐供给团队
     */
    private String team;
    /**
     * cpu 核数
     */
    private String numCpuCore;

    /**
     * cpu 信息
     */
    private String cpuInfo;
    /**
     * 内存 信息
     */
    private String memoryInfo;


    /**
     * 页码
     */
    private int pageNum = 1;
    /**
     * 每页条数
     */
    private int pageSize = 10;
}
