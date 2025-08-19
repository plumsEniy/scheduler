package com.bilibili.cluster.scheduler.common.dto.presto.tide;

import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import lombok.Data;

/**
 * @description: presto到presto工作流参数
 * @Date: 2025/5/15 15:16
 * @Author: nizhiqiang
 */

@Data
public class PrestoToPrestoTideExtFlowParams extends TideExtFlowParams {

    /**
     * 原版本上线的pod数量
     */
    private int sourceCurrentPod;

    /**
     * 原版本下线的pod数量
     */
    private int sourceRemainPod;

    /**
     * 目标版本上线的pod数量
     */
    private int sinkCurrentPod;

    /**
     * 目标版本下线的pod数量
     */
    private int sinkRemainPod;

//    private String tideOffStartTime;
//
//    private String tideOffEndTime;
//
//    private String tideOnStartTime;
//
//    private String tideOnEndTime;

    /**
     * 源集群的组件id
     */
    private long sourceComponentId;

    /**
     * 源集群的集群id
     */
    private long sourceClusterId;

    /**
     * 目标集群的组件id
     */
    private long sinkComponentId;

    /**
     * 目标集群的集群id
     */
    private long sinkClusterId;

    // nm组件id
//    private long componentId;
}
