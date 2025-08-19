package com.bilibili.cluster.scheduler.api.event.factory.impl.spark;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;

import java.util.Arrays;
import java.util.List;

public class SparkVersionLockEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.SPARK_VERSION_LOCK_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        final FlowDeployType deployType = pipelineParameter.getFlowEntity().getDeployType();
        final UnResolveEvent event = new UnResolveEvent();
        event.setScope(EventReleaseScope.EVENT);
        switch (deployType) {
            case SPARK_VERSION_LOCK:
                event.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_LOCK_EXEC_EVENT);
                break;
            default:
                event.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_RELEASE_EXEC_EVENT);
        }
        return Arrays.asList(event);
    }

}
