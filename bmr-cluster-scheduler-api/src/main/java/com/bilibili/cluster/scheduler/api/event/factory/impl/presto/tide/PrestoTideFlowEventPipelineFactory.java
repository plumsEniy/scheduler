package com.bilibili.cluster.scheduler.api.event.factory.impl.presto.tide;

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
public class PrestoTideFlowEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.PRESTO_TIDE_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        PrestoTidePipelineConf pipelineInfoConf = SpringApplicationContext.getBean(PrestoTidePipelineConf.class);
        log.info("prestoTideFlowPipelineInfo conf is {}", pipelineInfoConf);

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (Objects.isNull(req)) {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        } else {
            deployType = req.getDeployType();
        }

        switch (deployType) {
            case PRESTO_TIDE_OFF:
                return getPrestoTideOffEventList(pipelineInfoConf);
            case PRESTO_TIDE_ON:
                return getPrestoTideOnEventList(pipelineInfoConf);
            default:
                throw new IllegalArgumentException("un-support deployType of: " + deployType);
        }
    }

    private List<UnResolveEvent> getPrestoTideOffEventList(PrestoTidePipelineConf pipelineInfoConf) {

        final UnResolveEvent prestoPodShrinkEvent = new UnResolveEvent();
        prestoPodShrinkEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_POD_FAST_SHRINKAGE);
        prestoPodShrinkEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent waitNodesAvailableEvent = new UnResolveEvent();
        waitNodesAvailableEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_WAIT_AVAILABLE_NODES);
        waitNodesAvailableEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent deployYarnNodesEvent = new UnResolveEvent();
        deployYarnNodesEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_EXPANSION_YARN_NODES);
        deployYarnNodesEvent.setScope(EventReleaseScope.BATCH);
        deployYarnNodesEvent.setProjectCode(pipelineInfoConf.getProjectCode());
        deployYarnNodesEvent.setPipelineCode(pipelineInfoConf.getYarnExpansionPipelineId());
        deployYarnNodesEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);

        final UnResolveEvent updateServiceStateEvent = new UnResolveEvent();
        updateServiceStateEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_OFF_UPDATE_NODE_SERVICE_STATE);
        updateServiceStateEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // stage 1
                prestoPodShrinkEvent, waitNodesAvailableEvent,
                // stage 2
                deployYarnNodesEvent, updateServiceStateEvent);
    }

    private List<UnResolveEvent> getPrestoTideOnEventList(PrestoTidePipelineConf pipelineInfoConf) {
//        final UnResolveEvent captureYarnNodesEvent = new UnResolveEvent();
//        captureYarnNodesEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_CAPTURE_YARN_NODES);
//        captureYarnNodesEvent.setScope(EventReleaseScope.GLOBAL);

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

        final UnResolveEvent prestoPodExpansionEvent = new UnResolveEvent();
        prestoPodExpansionEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_POD_EXPANSION);
        prestoPodExpansionEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent prestoPodStatusCheckEvent = new UnResolveEvent();
        prestoPodStatusCheckEvent.setEventTypeEnum(EventTypeEnum.PRESTO_TIDE_ON_POD_STATUS_CHECK);
        prestoPodStatusCheckEvent.setScope(EventReleaseScope.BATCH);

        return Arrays.asList(
                // stage 1
                waitAppFinishEvent, evictionYarnNodesEvent, updateServiceStateEvent,
                // stage 2
                prestoPodExpansionEvent, prestoPodStatusCheckEvent);
    }

}
