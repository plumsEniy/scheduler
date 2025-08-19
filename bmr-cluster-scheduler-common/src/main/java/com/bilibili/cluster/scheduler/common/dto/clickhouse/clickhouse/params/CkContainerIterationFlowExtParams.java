package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2025/2/12 11:19
 * @Author: nizhiqiang
 */

@Data
public class CkContainerIterationFlowExtParams {

    String podTemplate;

    /**
     * 迭代的pod列表
     */
    List<String> iterationPodList;
}
