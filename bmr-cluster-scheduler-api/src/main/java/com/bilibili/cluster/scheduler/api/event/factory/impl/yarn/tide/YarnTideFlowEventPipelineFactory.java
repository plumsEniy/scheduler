package com.bilibili.cluster.scheduler.api.event.factory.impl.yarn.tide;

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
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class YarnTideFlowEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.YARN_TIDE_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        PrestoTidePipelineConf pipelineInfoConf = SpringApplicationContext.getBean(PrestoTidePipelineConf.class);
        log.info("yarn tide pipeline conf is {}", pipelineInfoConf);

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (Objects.isNull(req)) {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        } else {
            deployType = req.getDeployType();
        }

        switch (deployType) {
            case YARN_TIDE_SHRINK:
                return getYarnTideShrinkEventList(pipelineInfoConf);
            case YARN_TIDE_EXPANSION:
                return getYarnTideExpansionEventList(pipelineInfoConf);
            default:
                throw new IllegalArgumentException("un-support deployType of: " + deployType);
        }
    }

    private List<UnResolveEvent> getYarnTideShrinkEventList(PrestoTidePipelineConf pipelineInfoConf) {

        final UnResolveEvent waitAppFinishEvent = new UnResolveEvent();
        waitAppFinishEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_WAIT_APP_GRACEFUL_FINISH);
        waitAppFinishEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent evictionYarnNodesEvent = new UnResolveEvent();
        evictionYarnNodesEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_EVICTION_YARN_NODES);
        evictionYarnNodesEvent.setScope(EventReleaseScope.BATCH);
        evictionYarnNodesEvent.setProjectCode(pipelineInfoConf.getProjectCode());
        evictionYarnNodesEvent.setPipelineCode(pipelineInfoConf.getYarnEvictionPipelineId());
        evictionYarnNodesEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);

        final UnResolveEvent updateServiceStateEvent = new UnResolveEvent();
        updateServiceStateEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_UPDATE_NODE_SERVICE_STATE);
        updateServiceStateEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // require declare stage = 1
                waitAppFinishEvent, evictionYarnNodesEvent, updateServiceStateEvent);
    }

    private List<UnResolveEvent> getYarnTideExpansionEventList(PrestoTidePipelineConf pipelineInfoConf) {

        final UnResolveEvent preCheckEvent = new UnResolveEvent();
        preCheckEvent.setEventTypeEnum(EventTypeEnum.PRESTO_YARN_TIDE_EXPANSION_PRE_CHECK);
        preCheckEvent.setScope(EventReleaseScope.STAGE);

        final UnResolveEvent waitNodeAvailableEvent = new UnResolveEvent();
        waitNodeAvailableEvent.setEventTypeEnum(EventTypeEnum.PRESTO_YARN_TIDE_EXPANSION_WAITING_AVAILABLE_NODES);
        waitNodeAvailableEvent.setScope(EventReleaseScope.STAGE);

        final UnResolveEvent expansionMainEvent = new UnResolveEvent();
        expansionMainEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_EXPANSION_YARN_NODES);
        expansionMainEvent.setScope(EventReleaseScope.BATCH);
        expansionMainEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        expansionMainEvent.setProjectCode(pipelineInfoConf.getProjectCode());
        expansionMainEvent.setPipelineCode(pipelineInfoConf.getYarnExpansionPipelineId());

        final UnResolveEvent updateStateEvent = new UnResolveEvent();
        updateStateEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_UPDATE_NODE_SERVICE_STATE);
        updateStateEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // stage 1
                preCheckEvent, waitNodeAvailableEvent,
                // stage 2
                expansionMainEvent, updateStateEvent
        );
    }

}
