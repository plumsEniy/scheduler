package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;


import com.bilibili.cluster.scheduler.api.event.tide.AbstractYarnNodeShrinkEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CkYarnNodeShrinkEventHandler extends AbstractYarnNodeShrinkEventHandler {


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_ON_EVICTION_YARN_NODES;
    }
}
