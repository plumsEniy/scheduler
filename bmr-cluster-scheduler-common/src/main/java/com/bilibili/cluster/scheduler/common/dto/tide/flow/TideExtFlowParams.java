package com.bilibili.cluster.scheduler.common.dto.tide.flow;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/3/21 14:20
 * @Author: nizhiqiang
 */

@Data
public class TideExtFlowParams {

    private String appId;

    private long yarnClusterId;

    /**
     * 是否生成虚拟节点
     */
    private boolean generateNode;
}
