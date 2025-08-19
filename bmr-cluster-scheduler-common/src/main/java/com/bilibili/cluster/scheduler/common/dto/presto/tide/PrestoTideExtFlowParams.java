package com.bilibili.cluster.scheduler.common.dto.presto.tide;


import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import lombok.Data;

@Data
public class PrestoTideExtFlowParams extends TideExtFlowParams {

    /**
     * 潮汐上线的presto数量
     */
    private int currentPod;

    /**
     * 潮汐下线的presto数量
     */
    private int remainPod;

    private String tideOffStartTime;

    private String tideOffEndTime;

    private String tideOnStartTime;

    private String tideOnEndTime;

    // nm组件id
    private long componentId;

    // 潮汐配置id
    private int configId;

}
