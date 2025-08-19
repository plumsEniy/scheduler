package com.bilibili.cluster.scheduler.common.dto.tide.conf;

import lombok.Data;

@Data
public class TideConfDTO {

    private int highPodNum;

    private int lowPodNum;

    private long clusterId;

    private long componentId;

    private String appId;

}
