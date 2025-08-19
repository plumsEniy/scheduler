package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import com.bilibili.cluster.scheduler.api.event.tide.AbstractOnNodeStatusUpdateEventHandler;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.resourceV2.TideClusterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CkOnNodeStatusUpdateEventHandler extends AbstractOnNodeStatusUpdateEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_ON_UPDATE_NODE_SERVICE_STATE;
    }

    @Override
    protected long getTideOffCasterClusterId() {
        return Constants.CK_K8S_CLUSTER_ID;
    }

    @Override
    protected TideClusterType getTideClusterType() {
        return TideClusterType.CLICKHOUSE;
    }

    @Override
    protected String getDeployService() {
        return "clickhouse";
    }
}
