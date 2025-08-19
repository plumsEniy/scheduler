
package com.bilibili.cluster.scheduler.api.event.factory.impl;


import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;

import java.util.Arrays;
import java.util.List;

public class AmiyaFlowEventPipelineFactory extends AbstractPipelineFactory {

    public static final String IDENTIFIER = "Amiya";

    public AmiyaFlowEventPipelineFactory() {
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        FlowDeployType deployType = pipelineParameter.getFlowEntity().getDeployType();

        switch (deployType) {
            case CAPACITY_EXPANSION:
                return getExpansionPipelineEvents(pipelineParameter);
            default:
                return getDefaultPipelineEvents(pipelineParameter);
        }
    }

    private List<UnResolveEvent> getExpansionPipelineEvents(PipelineParameter pipelineParameter) {

        UnResolveEvent randomEvent = new UnResolveEvent();
        randomEvent.setEventTypeEnum(EventTypeEnum.RANDOM_TEST_EXEC_EVENT);
        randomEvent.setScope(EventReleaseScope.EVENT);

        UnResolveEvent dolphinEvent1 = new UnResolveEvent();
        dolphinEvent1.setEventTypeEnum(EventTypeEnum.DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT);
        dolphinEvent1.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        dolphinEvent1.setScope(EventReleaseScope.BATCH);
        dolphinEvent1.setGroupIndex(1);

        UnResolveEvent batchAlignEvent = new UnResolveEvent();
        batchAlignEvent.setEventTypeEnum(EventTypeEnum.BATCH_ALIGN_TEST_EXEC_EVENT);
        batchAlignEvent.setScope(EventReleaseScope.BATCH);

        UnResolveEvent dolphinEvent2 = new UnResolveEvent();
        dolphinEvent2.setEventTypeEnum(EventTypeEnum.DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT);
        dolphinEvent2.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        dolphinEvent2.setScope(EventReleaseScope.BATCH);
        dolphinEvent2.setGroupIndex(2);

        UnResolveEvent refreshEvent = new UnResolveEvent();
        refreshEvent.setEventTypeEnum(EventTypeEnum.REFRESH_RESOURCE_MANAGER_INFO_EVENT);
        refreshEvent.setScope(EventReleaseScope.BATCH);

        return Arrays.asList(randomEvent, dolphinEvent1, batchAlignEvent, dolphinEvent2, refreshEvent);
    }

    private List<UnResolveEvent> getDefaultPipelineEvents(PipelineParameter pipelineParameter) {
        UnResolveEvent dolphinEvent = new UnResolveEvent();
        dolphinEvent.setEventTypeEnum(EventTypeEnum.DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT);
        dolphinEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        dolphinEvent.setScope(EventReleaseScope.BATCH);
        dolphinEvent.setGroupIndex(0);

        UnResolveEvent refreshEvent = new UnResolveEvent();
        refreshEvent.setEventTypeEnum(EventTypeEnum.REFRESH_RESOURCE_MANAGER_INFO_EVENT);
        refreshEvent.setScope(EventReleaseScope.BATCH);

        return Arrays.asList(dolphinEvent, refreshEvent);
    }

}
