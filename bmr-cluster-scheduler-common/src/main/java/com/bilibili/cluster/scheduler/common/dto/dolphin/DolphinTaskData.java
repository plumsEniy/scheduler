package com.bilibili.cluster.scheduler.common.dto.dolphin;

import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinNodeStatus;
import lombok.Data;

/**
 * @description:
 * @Date: 2024/5/16 14:11
 * @Author: nizhiqiang
 */

@Data
public class DolphinTaskData {
    private String taskCode;
    private String taskName;
    private DolphinNodeStatus status = DolphinNodeStatus.READY;

    /**
     * 是否持久化日志
     */
    private boolean isPersistenceReady;

    public DolphinTaskData(String taskCode, String taskName) {
        this.taskCode = taskCode;
        this.taskName = taskName;
    }
}
