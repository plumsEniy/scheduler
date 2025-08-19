package com.bilibili.cluster.scheduler.api.event.presto.tide;


import com.bilibili.cluster.scheduler.api.event.tide.AbstractYarnNodeShrinkEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrestoYarnNodeShrinkEventHandler extends AbstractYarnNodeShrinkEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_ON_EVICTION_YARN_NODES;
    }

}
