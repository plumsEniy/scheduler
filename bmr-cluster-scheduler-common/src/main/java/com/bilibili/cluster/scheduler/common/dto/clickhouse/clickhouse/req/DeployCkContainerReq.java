package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.req;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2025/2/12 15:12
 * @Author: nizhiqiang
 */

@Data

public class DeployCkContainerReq {

    private Long componentId;

    private Long clusterId;

    private long packageId;

    private long configId;

    private List<Integer> shardAllocationList;

    private List<String> nodeList;

}
