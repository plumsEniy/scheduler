package com.bilibili.cluster.scheduler.api.event.presto.tide;

import com.bilibili.cluster.scheduler.api.event.tide.AbstractTideOffNodeStatusUpdateEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrestoOffNodeStatusUpdateEventHandler extends AbstractTideOffNodeStatusUpdateEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_OFF_UPDATE_NODE_SERVICE_STATE;
    }

    @Override
    protected boolean skipCheckEventIsRequired() {
        return false;
    }

    @Override
    protected TideClusterType getTideClusterType() {
        return TideClusterType.PRESTO;
    }
}
