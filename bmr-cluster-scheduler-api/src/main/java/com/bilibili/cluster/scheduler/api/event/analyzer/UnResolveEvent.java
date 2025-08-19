package com.bilibili.cluster.scheduler.api.event.analyzer;

import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import lombok.Data;

@Data
public class UnResolveEvent {

    // 必传参数
    private EventTypeEnum eventTypeEnum;
    // 必传参数
    private EventReleaseScope scope;


    // dolphin-pipeline required
    private String projectCode;

    private String pipelineCode;

    /**
     * 可选参数（dolphin类型必须）
     * 需要为自然数
     * 默认 0，表示该流程中只有一段dolphin-pipeline
     * 其他表示有多段dolphin-pipeline处理流程
     */
    private int groupIndex;
    /**
     * 可选参数（dolphin类型必须）
     * dolphin-scheduler失败处理策略
     * 继续 or 终止
     */
    private DolpFailureStrategy failureStrategy;

}
