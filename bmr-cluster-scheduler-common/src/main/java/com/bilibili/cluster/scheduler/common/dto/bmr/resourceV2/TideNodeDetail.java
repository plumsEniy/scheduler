package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2;

import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideNodeStatus;
import lombok.Data;

@Data
public class TideNodeDetail {

    private String appId;
    private TideNodeStatus casterStatus;
    private int coreNum;
    private String memory;
    private String deployService;
    private String hostName;
    private String ip;

}
