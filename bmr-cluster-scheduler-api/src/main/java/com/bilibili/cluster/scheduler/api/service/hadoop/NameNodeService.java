package com.bilibili.cluster.scheduler.api.service.hadoop;

import com.bilibili.cluster.scheduler.common.dto.hadoop.NameNodeFsIndex;

import java.util.List;

/**
 * @description: nameNode服务
 * @Date: 2024/5/13 15:04
 * @Author: nizhiqiang
 */
public interface NameNodeService {

    /**
     * 查询namenode的索引
     * @param nameNodeHostName
     * @return
     */
    List<NameNodeFsIndex> queryNameNodeFsIndex(String nameNodeHostName);
}
