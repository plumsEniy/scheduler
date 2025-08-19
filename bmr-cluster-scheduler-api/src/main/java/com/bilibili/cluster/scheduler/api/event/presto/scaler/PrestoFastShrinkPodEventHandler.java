package com.bilibili.cluster.scheduler.api.event.presto.scaler;

import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoDeployEventHandler;
import com.bilibili.cluster.scheduler.common.dto.presto.scaler.PrestoFastScalerExtFlowParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrestoFastShrinkPodEventHandler extends AbstractTidePrestoDeployEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_POD_FAST_SHRINK;
    }

    @Override
    protected Integer getNeedScalePodCount(TaskEvent taskEvent) {
        final Long flowId = taskEvent.getFlowId();
        final PrestoFastScalerExtFlowParams extFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoFastScalerExtFlowParams.class);
        Preconditions.checkNotNull(extFlowParams, "extFlowParams is null");
        return extFlowParams.getLowPodNum();
    }
}
