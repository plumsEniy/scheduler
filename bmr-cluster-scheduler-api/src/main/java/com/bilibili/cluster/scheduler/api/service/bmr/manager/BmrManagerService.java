package com.bilibili.cluster.scheduler.api.service.bmr.manager;

/**
 * @description:
 * @Date: 2024/8/13 16:20
 * @Author: nizhiqiang
 */
public interface BmrManagerService {

    /**
     * 刷新bmr上的pod信息
     * @param clusterId
     * @param componentId
     * @param clusterName
     */
    void refreshPodInfo(Long clusterId,Long componentId ,String clusterName);
}
