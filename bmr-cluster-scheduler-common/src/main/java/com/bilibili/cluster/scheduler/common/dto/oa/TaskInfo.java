package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

/**
 * @description:
 * @Date: 2024/3/6 15:47
 * @Author: nizhiqiang
 */
@Data
public class TaskInfo {

    private String actorIds;
    private String operator;
    private String taskCreateTime;
    private String taskDisplayName;
    private String taskId;
    private String taskName;
}
