package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.expansion;

import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @Date: 2025/4/29 15:41
 * @Author: nizhiqiang
 */

@Component
public class NNproxyExpansionPipelineEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_EXPANSION_PIPELINE_EVENT;
    }
}
