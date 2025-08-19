package com.bilibili.cluster.scheduler.api.event.factory.impl;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;

import java.util.Arrays;
import java.util.List;

public class MonitorFlowEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.MONITOR_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        final UnResolveEvent monitorEvent = new UnResolveEvent();
        monitorEvent.setEventTypeEnum(EventTypeEnum.MONITOR_OBJECT_CHANGE_EXEC_EVENT);
        monitorEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(monitorEvent);
    }
}
