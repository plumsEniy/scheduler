package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params;


import com.bilibili.cluster.scheduler.common.dto.tide.flow.TideExtFlowParams;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
public class CkTideExtFlowParams extends TideExtFlowParams {

    /**
     * 潮汐上线的ck数量
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
    private long nodeManagerComponentId;

    // 潮汐配置id
    private long configId;

    //    实际潮汐pod数
    private int actualTidePodCount = 0;

    //    实际留下pod数
    private int actualRemainPodCount;

    /**
     * 主机移除pod的map
     */
    private Map<String, Set<String>> hostIpToRemovePodMap = new HashMap<>();
}
