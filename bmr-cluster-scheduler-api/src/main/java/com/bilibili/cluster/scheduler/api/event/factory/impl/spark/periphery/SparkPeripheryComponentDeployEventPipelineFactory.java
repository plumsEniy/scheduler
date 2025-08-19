package com.bilibili.cluster.scheduler.api.event.factory.impl.spark.periphery;

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

public class SparkPeripheryComponentDeployEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.SPARK_PERIPHERY_COMPONENT_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        FlowDeployType deployType;
        final DeployOneFlowReq req = pipelineParameter.getReq();
        if (!Objects.isNull(req)) {
            deployType = req.getDeployType();
        } else {
            deployType = pipelineParameter.getFlowEntity().getDeployType();
        }

        switch (deployType) {
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
                return getDeployEventList();
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                return getLockEventList();
            default:
                throw new IllegalArgumentException("un-support deploy type: " + deployType);
        }

    }

    private List<UnResolveEvent> getDeployEventList() {
        final UnResolveEvent preCheck = new UnResolveEvent();
        preCheck.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_PRE_CHECK);
        preCheck.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent deployEvent = new UnResolveEvent();
        deployEvent.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_UPDATE_VERSION);
        deployEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent stageCheck = new UnResolveEvent();
        stageCheck.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_DEPLOY_STAGE_CHECK);
        stageCheck.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(preCheck, deployEvent, stageCheck);
    }

    private List<UnResolveEvent> getLockEventList() {
        final UnResolveEvent preCheck = new UnResolveEvent();
        preCheck.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_PRE_CHECK);
        preCheck.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent lockEvent = new UnResolveEvent();
        lockEvent.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_VERSION_UPDATE);
        lockEvent.setScope(EventReleaseScope.EVENT);

        final UnResolveEvent stageCheck = new UnResolveEvent();
        stageCheck.setEventTypeEnum(EventTypeEnum.SPARK_PERIPHERY_COMPONENT_LOCK_STAGE_CHECK);
        stageCheck.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(preCheck, lockEvent, stageCheck);
    }
}
