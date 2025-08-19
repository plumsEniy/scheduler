package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeStateUpdateReq {

    private String casterStatus;

    private String appId;

    private String hostName;

    private String deployService;

    private TideClusterType belongResourcePool;

}
