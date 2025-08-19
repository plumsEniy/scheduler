package com.bilibili.cluster.scheduler.common.enums.presto;

import com.bilibili.cluster.scheduler.common.Constants;

/**
 * @description: pod类型
 * @Date: 2024/12/5 10:58
 * @Author: nizhiqiang
 */


public enum PodType {
    WORKER,
    COORDINATOR,
    RESOURCE,
    UNKNOWN;

    public static PodType getPodType(String podName) {
        if (podName.contains(Constants.WORKER)) {
            return WORKER;
        } else if (podName.contains(Constants.RESOURCE)) {
            return RESOURCE;
        } else if (podName.contains(Constants.COORDINATOR)) {
            return COORDINATOR;
        }
        return UNKNOWN;
    }
}
