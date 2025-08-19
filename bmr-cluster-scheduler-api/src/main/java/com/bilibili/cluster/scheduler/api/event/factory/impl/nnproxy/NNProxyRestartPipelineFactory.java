package com.bilibili.cluster.scheduler.api.event.factory.impl.nnproxy;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.conf.NNProxyRestartPipelineConf;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NNProxyRestartPipelineFactory extends AbstractPipelineFactory {

    private NNProxyRestartPipelineConf pipelineConf;

    public NNProxyRestartPipelineFactory() {
        super();
        pipelineConf = SpringApplicationContext.getBean(NNProxyRestartPipelineConf.class);
        log.info("NNProxyDeployPipelineFactory pipelineConf is {}", pipelineConf);
    }

    @Override
    public String identifier() {
        return Constants.NN_PROXY_RESTART_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        List<UnResolveEvent> events = new ArrayList<>();
        // 扩容主流程
        final UnResolveEvent expansionPipelineEvent = new UnResolveEvent();
        expansionPipelineEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_RESTART_PIPELINE_EVENT);
        expansionPipelineEvent.setScope(EventReleaseScope.EVENT);
        expansionPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        expansionPipelineEvent.setPipelineCode(pipelineConf.getRestartPipelineId());
        expansionPipelineEvent.setFailureStrategy(DolpFailureStrategy.END);
        events.add(expansionPipelineEvent);
        return events;
    }
}
