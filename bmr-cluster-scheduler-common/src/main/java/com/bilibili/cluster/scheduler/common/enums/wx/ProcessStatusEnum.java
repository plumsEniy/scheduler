package com.bilibili.cluster.scheduler.common.enums.wx;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 流程状态
 * @Date: 2024/3/19 14:42
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum ProcessStatusEnum {
    PROCESSING("process_deliver_doing"),
    COMPLETE("process_deliver_completed"),
    FAILED("process_deliver_failed");

    @Getter
    private String type;

}
