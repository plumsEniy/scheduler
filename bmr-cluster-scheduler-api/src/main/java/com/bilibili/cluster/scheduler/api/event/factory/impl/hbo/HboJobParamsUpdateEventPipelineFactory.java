package com.bilibili.cluster.scheduler.api.event.factory.impl.hbo;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @Date: 2024/12/30 15:53
 * @Author: nizhiqiang
 */

@Slf4j
public class HboJobParamsUpdateEventPipelineFactory extends AbstractPipelineFactory {
    @Override
    public String identifier() {
        return Constants.HBO_JOB_PARAMS_UPDATE_DEPLOY_FACTORY_IDENEITFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        final UnResolveEvent monitorEvent = new UnResolveEvent();
        monitorEvent.setEventTypeEnum(EventTypeEnum.HBO_JOB_PARAMS_UPDATE);
        monitorEvent.setScope(EventReleaseScope.EVENT);

        return Arrays.asList(monitorEvent);
    }
}
