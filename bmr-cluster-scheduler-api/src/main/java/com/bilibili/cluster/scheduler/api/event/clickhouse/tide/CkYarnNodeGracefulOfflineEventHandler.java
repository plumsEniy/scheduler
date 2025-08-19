package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;


import com.bilibili.cluster.scheduler.api.event.tide.AbstractTideYarnNodeGracefulOfflineEventHandler;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CkYarnNodeGracefulOfflineEventHandler extends AbstractTideYarnNodeGracefulOfflineEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_ON_WAIT_APP_GRACEFUL_FINISH;
    }


}
