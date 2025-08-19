package com.bilibili.cluster.scheduler.api.event.factory.impl.clickhouse;

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
 * @Date: 2025/2/11 20:04
 * @Author: nizhiqiang
 */

@Slf4j
public class CkContainerEventPipelineFactory extends AbstractPipelineFactory {
    @Override
    public String identifier() {
        return Constants.CK_CONTAINER_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        UnResolveEvent deployCkTemplate = new UnResolveEvent();
        deployCkTemplate.setEventTypeEnum(EventTypeEnum.CK_CONTAINER_DEPLOY);
        deployCkTemplate.setScope(EventReleaseScope.GLOBAL);

        UnResolveEvent checkCkContainer = new UnResolveEvent();
        checkCkContainer.setEventTypeEnum(EventTypeEnum.CK_CHECK_CONTAINER);
        checkCkContainer.setScope(EventReleaseScope.BATCH);

        return Arrays.asList(deployCkTemplate, checkCkContainer);
    }
}
