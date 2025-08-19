package com.bilibili.cluster.scheduler.common.dto.bmr.resource.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddHostToNodeGroupReq {

    private long clusterId;

    private List<String> hostList;

    private String groupName;

}
