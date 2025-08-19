package com.bilibili.cluster.scheduler.common.dto.presto;

import lombok.Data;

/**
 * @description: presto节点信息
 * @Date: 2024/7/30 17:57
 * @Author: nizhiqiang
 */
@Data
public class PrestoNodeInfo {
    private PrestoConfig coordinator;
    private PrestoConfig resource;
    private PrestoConfig worker;

}
