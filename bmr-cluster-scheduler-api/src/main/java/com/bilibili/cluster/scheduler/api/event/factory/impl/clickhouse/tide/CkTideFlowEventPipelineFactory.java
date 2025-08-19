package com.bilibili.cluster.scheduler.api.event.factory.impl.clickhouse.tide;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.clickhouse.conf.CkTidePipelineConf;
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
public class CkTideFlowEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.CK_TIDE_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        CkTidePipelineConf pipelineInfoConf = SpringApplicationContext.getBean(CkTidePipelineConf.class);
        log.info("CkTideFlowPipelineInfo conf is {}", pipelineInfoConf);

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (Objects.isNull(req)) {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        } else {
            deployType = req.getDeployType();
        }

        switch (deployType) {
            case CK_TIDE_OFF:
                return getCkTideOffEventList(pipelineInfoConf);
            case CK_TIDE_ON:
                return getCkTideOnEventList(pipelineInfoConf);
            default:
                throw new IllegalArgumentException("un-support deployType of: " + deployType);
        }
    }

    private List<UnResolveEvent> getCkTideOffEventList(CkTidePipelineConf pipelineInfoConf) {

        final UnResolveEvent ckPodShrinkEvent = new UnResolveEvent();
        ckPodShrinkEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_OFF_POD_FAST_SHRINKAGE);
        ckPodShrinkEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent waitNodesAvailableEvent = new UnResolveEvent();
        waitNodesAvailableEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_OFF_WAIT_AVAILABLE_NODES);
        waitNodesAvailableEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent killPvcEvent = new UnResolveEvent();
        killPvcEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_OFF_KILL_PVC);
        killPvcEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent deployYarnNodesEvent = new UnResolveEvent();
        deployYarnNodesEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_OFF_EXPANSION_YARN_NODES);
        deployYarnNodesEvent.setScope(EventReleaseScope.BATCH);
        deployYarnNodesEvent.setProjectCode(pipelineInfoConf.getProjectCode());
        deployYarnNodesEvent.setPipelineCode(pipelineInfoConf.getYarnDeployPipelineId());
        deployYarnNodesEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);

        final UnResolveEvent updateServiceStateEvent = new UnResolveEvent();
        updateServiceStateEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_OFF_UPDATE_NODE_SERVICE_STATE);
        updateServiceStateEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(
                // stage 1
                ckPodShrinkEvent, waitNodesAvailableEvent, killPvcEvent,
                // stage 2
                deployYarnNodesEvent, updateServiceStateEvent);
    }

    private List<UnResolveEvent> getCkTideOnEventList(CkTidePipelineConf pipelineInfoConf) {
//        final UnResolveEvent captureYarnNodesEvent = new UnResolveEvent();
//        captureYarnNodesEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_CAPTURE_YARN_NODES);
//        captureYarnNodesEvent.setScope(EventReleaseScope.GLOBAL);

        final UnResolveEvent waitAppFinishEvent = new UnResolveEvent();
        waitAppFinishEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_WAIT_APP_GRACEFUL_FINISH);
        waitAppFinishEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent evictionYarnNodesEvent = new UnResolveEvent();
        evictionYarnNodesEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_EVICTION_YARN_NODES);
        evictionYarnNodesEvent.setScope(EventReleaseScope.BATCH);
        evictionYarnNodesEvent.setProjectCode(pipelineInfoConf.getProjectCode());
        evictionYarnNodesEvent.setPipelineCode(pipelineInfoConf.getYarnEvictionPipelineId());
        evictionYarnNodesEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);

        final UnResolveEvent updateServiceStateEvent = new UnResolveEvent();
        updateServiceStateEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_UPDATE_NODE_SERVICE_STATE);
        updateServiceStateEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent ckPodExpansionEvent = new UnResolveEvent();
        ckPodExpansionEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_POD_EXPANSION);
        ckPodExpansionEvent.setScope(EventReleaseScope.BATCH);

        final UnResolveEvent ckPodStatusCheckEvent = new UnResolveEvent();
        ckPodStatusCheckEvent.setEventTypeEnum(EventTypeEnum.CK_TIDE_ON_POD_STATUS_CHECK);
        ckPodStatusCheckEvent.setScope(EventReleaseScope.BATCH);

        return Arrays.asList(
                // stage 1
                waitAppFinishEvent, evictionYarnNodesEvent, updateServiceStateEvent,
                // stage 2
                ckPodExpansionEvent, ckPodStatusCheckEvent);
    }

}
