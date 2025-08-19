package com.bilibili.cluster.scheduler.api.event.zk;

import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import org.springframework.stereotype.Component;

/**
 * @description: zk重启流程
 * @Date: 2025/7/3 15:36
 * @Author: nizhiqiang
 */

@Component
public class ZkRestartEventHandler extends AbstractDolphinSchedulerEventHandler {
    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.ZK_RESTART;
    }
}
