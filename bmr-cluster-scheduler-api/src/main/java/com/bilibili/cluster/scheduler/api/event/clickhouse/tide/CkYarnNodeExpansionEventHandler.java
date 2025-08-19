package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import com.bilibili.cluster.scheduler.api.event.tide.AbstractTideYarnNodeExpansionEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CkYarnNodeExpansionEventHandler extends AbstractTideYarnNodeExpansionEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_OFF_EXPANSION_YARN_NODES;
    }

    @Override
    protected boolean skipCheckEventIsRequired() {
        return false;
    }

    @Override
    protected String getYarnHostGroupName() {
        return Constants.CK_TIDE_NODE_GROUP_NAME;
    }
}
