package com.bilibili.cluster.scheduler.api.event.factory.impl.presto.scaler;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PrestoFastScalerEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.PRESTO_FAST_SCALER_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (Objects.isNull(req)) {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        } else {
            deployType = req.getDeployType();
        }

        switch (deployType) {
            case PRESTO_FAST_SHRINK:
                return getPrestoFastShrinkEventList();
            case PRESTO_FAST_EXPANSION:
                return getPrestoFastExpansionEventList();
            default:
                throw new IllegalArgumentException("un-support deployType of: " + deployType);
        }
    }

    private List<UnResolveEvent> getPrestoFastShrinkEventList() {
        final UnResolveEvent prestoPodShrinkEvent = new UnResolveEvent();
        prestoPodShrinkEvent.setEventTypeEnum(EventTypeEnum.PRESTO_POD_FAST_SHRINK);
        prestoPodShrinkEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoShrinkWaitEvent = new UnResolveEvent();
        prestoShrinkWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_POD_SHRINKAGE_WAIT_READY);
        prestoShrinkWaitEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(prestoPodShrinkEvent, prestoShrinkWaitEvent);
    }

    private List<UnResolveEvent> getPrestoFastExpansionEventList() {
        final UnResolveEvent prestoPodExpansionEvent = new UnResolveEvent();
        prestoPodExpansionEvent.setEventTypeEnum(EventTypeEnum.PRESTO_POD_FAST_EXPANSION);
        prestoPodExpansionEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoExpansionWaitEvent = new UnResolveEvent();
        prestoExpansionWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_POD_EXPANSION_WAIT_READY);
        prestoExpansionWaitEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(prestoPodExpansionEvent, prestoExpansionWaitEvent);
    }

}
