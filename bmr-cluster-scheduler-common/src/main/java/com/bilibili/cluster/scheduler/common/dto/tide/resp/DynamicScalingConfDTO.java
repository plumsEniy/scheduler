package com.bilibili.cluster.scheduler.common.dto.tide.resp;

import com.bilibili.cluster.scheduler.common.dto.tide.type.DynamicScalingStrategy;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.Data;

@Data
public class DynamicScalingConfDTO {

    // 是否开启了定时任务
    private boolean scalingState;

    private String appId;

    private long clusterId;

    private String clusterName;

    private long componentId;

    private String componentName;

    private TideClusterType configBelong;

    private String creator;

    private DynamicScalingStrategy dynamicScalingStrategy;

    private String expansionTimeStart;

    private int highPeakNodeNum;

    private boolean isSkipHolidayScaling;

    private int lowPeakNodeNum;

    private String shrinkTimeStart;

    private String shrinkTimeEnd;

}
