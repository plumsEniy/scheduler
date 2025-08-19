package com.bilibili.cluster.scheduler.api.event.factory.impl.presto.toPrestoTide;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto.conf.PrestoTidePipelineConf;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: presto到presto
 * @Date: 2025/5/15 15:48
 * @Author: nizhiqiang
 */

@Slf4j
public class PrestoToPrestoTideFlowEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.PRESTO_TO_PRESTO_TIDE_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        PrestoTidePipelineConf pipelineInfoConf = SpringApplicationContext.getBean(PrestoTidePipelineConf.class);
        log.info("prestoToPrestoTideFlowPipelineInfo conf is {}", pipelineInfoConf);

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (Objects.isNull(req)) {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        } else {
            deployType = req.getDeployType();
        }

        switch (deployType) {
            case PRESTO_TO_PRESTO_TIDE_OFF:
                return getPrestoTideOffEventList(pipelineInfoConf);
            case PRESTO_TO_PRESTO_TIDE_ON:
                return getPrestoTideOnEventList(pipelineInfoConf);
            default:
                throw new IllegalArgumentException("un-support deployType of: " + deployType);
        }
    }

    private List<UnResolveEvent> getPrestoTideOffEventList(PrestoTidePipelineConf pipelineInfoConf) {
        final UnResolveEvent prestoPodShrinkEvent = new UnResolveEvent();
        prestoPodShrinkEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_OFF_POD_FAST_SHRINKAGE);
        prestoPodShrinkEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoShrinkWaitEvent = new UnResolveEvent();
        prestoShrinkWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_OFF_WAIT_SHRINKAGE_POD);
        prestoShrinkWaitEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoPodExpansionEvent = new UnResolveEvent();
        prestoPodExpansionEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_OFF_POD_EXPANSION);
        prestoPodExpansionEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoExpansionWaitEvent = new UnResolveEvent();
        prestoExpansionWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_OFF_WAIT_EXPANSION_POD);
        prestoExpansionWaitEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // stage 1
                prestoPodShrinkEvent, prestoShrinkWaitEvent,
                // stage 2
                prestoPodExpansionEvent, prestoExpansionWaitEvent);
    }

    private List<UnResolveEvent> getPrestoTideOnEventList(PrestoTidePipelineConf pipelineInfoConf) {
        final UnResolveEvent prestoPodShrinkEvent = new UnResolveEvent();
        prestoPodShrinkEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_ON_POD_FAST_SHRINKAGE);
        prestoPodShrinkEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoShrinkWaitEvent = new UnResolveEvent();
        prestoShrinkWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_ON_WAIT_SHRINKAGE_POD);
        prestoShrinkWaitEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoPodExpansionEvent = new UnResolveEvent();
        prestoPodExpansionEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_ON_POD_EXPANSION);
        prestoPodExpansionEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent prestoExpansionWaitEvent = new UnResolveEvent();
        prestoExpansionWaitEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TO_PRESTO_TIDE_ON_WAIT_EXPANSION_POD);
        prestoExpansionWaitEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // stage 1
                prestoPodShrinkEvent, prestoShrinkWaitEvent,
                // stage 2
                prestoPodExpansionEvent, prestoExpansionWaitEvent);
    }
}
