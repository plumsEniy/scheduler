package com.bilibili.cluster.scheduler.api.event.factory.impl.spark;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;

import java.util.Arrays;
import java.util.List;

public class SparkExperimentEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.SPARK_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        final UnResolveEvent createExperimentEvent = new UnResolveEvent();
        createExperimentEvent.setScope(EventReleaseScope.EVENT);
        createExperimentEvent.setEventTypeEnum(EventTypeEnum.SPARK_EXPERIMENT_CREATE_EXEC_EVENT);

        final UnResolveEvent queryExperimentResultEvent = new UnResolveEvent();
        queryExperimentResultEvent.setScope(EventReleaseScope.EVENT);
        queryExperimentResultEvent.setEventTypeEnum(EventTypeEnum.SPARK_EXPERIMENT_QUERY_EXEC_EVENT);

        return Arrays.asList(createExperimentEvent, queryExperimentResultEvent);
    }
}
