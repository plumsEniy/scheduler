package com.bilibili.cluster.scheduler.common.dto.oa;

import lombok.Data;

import java.util.List;

/**
 * @description: oa历史
 * @Date: 2024/3/6 14:29
 * @Author: nizhiqiang
 */
@Data
public class OAHistory {
    private String actorDept;
    private String actorIcon;
    private String actorUserName;
    private int agent;
    private String comment;
    private List<String> commentFile;
    private String orderId;
    private String taskAction;
    private String taskDisplayName;
    private String taskFinishTime;
    private String taskId;
    private String taskName;
}
