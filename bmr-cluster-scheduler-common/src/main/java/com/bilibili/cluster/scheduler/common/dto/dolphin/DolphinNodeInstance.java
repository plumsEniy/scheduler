package com.bilibili.cluster.scheduler.common.dto.dolphin;

import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinNodeStatus;
import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @Date: 2024/5/16 10:56
 * @Author: nizhiqiang
 */

@Data
public class DolphinNodeInstance {
    private String hostname;
    private DolphinNodeStatus status = DolphinNodeStatus.READY;

    private Map<String, DolphinTaskData> taskCodeToDataMap;

    public DolphinNodeInstance(String hostname) {
        this.hostname = hostname;
    }
}
