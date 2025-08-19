package com.bilibili.cluster.scheduler.common.dolphin;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DolphinPipelineInstance {

    /**
     * dolphinScheduler required props
     */
    private String projectCode;

    private String pipelineCode;

    // 流程命名索引
    private String schedulerPipelineChainIndex;

    // 执行实例Id
    private String schedulerInstanceId;

    // 流程节点id列表
    private List<String> pipelineTaskCodeList;

    /**
     * key -> 流程节点id
     * value -> 流程节点名称
     */
    private Map<String, String> pipelineTaskCodeToTaskNameMap;

    /**
     * key -> 流程节点id
     * value -> job-agent taskSet Id
     */
    private Map<String, Long> pipelineTaskCodeToJobTaskSetIdMap;


}
