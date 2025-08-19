package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2024/5/15 15:32
 * @Author: nizhiqiang
 */

@Data
public class QueryNodeGroupInfoReq {
    private long clusterID;
    private List<String> hostNameList;

    public QueryNodeGroupInfoReq(long clusterID, List<String> hostNameList) {
        this.clusterID = clusterID;
        this.hostNameList = hostNameList;
    }
}
