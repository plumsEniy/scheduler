package com.bilibili.cluster.scheduler.api.event.presto.tide;

import com.bilibili.cluster.scheduler.api.event.tide.AbstractOnNodeStatusUpdateEventHandler;
import com.bilibili.cluster.scheduler.api.service.presto.PrestoService;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class PrestoOnNodeStatusUpdateEventHandler extends AbstractOnNodeStatusUpdateEventHandler {

    @Resource
    PrestoService prestoService;


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_ON_UPDATE_NODE_SERVICE_STATE;
    }

    @Override
    protected long getTideOffCasterClusterId() {
        return prestoService.getPrestoCasterClusterId();
    }

    @Override
    protected TideClusterType getTideClusterType() {
        return TideClusterType.PRESTO;
    }

    @Override
    protected String getDeployService() {
        return "Presto";
    }
}
