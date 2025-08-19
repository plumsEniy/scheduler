package com.bilibili.cluster.scheduler.api.event.factory.impl;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.zookeeper.conf.ZkDeployPipelineConf;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: zk发布
 * @Date: 2025/6/25 19:35
 * @Author: nizhiqiang
 */
public class ZookeeperFlowEventPipelineFactory extends AbstractPipelineFactory {


    private ZkDeployPipelineConf pipelineConf;


    public ZookeeperFlowEventPipelineFactory() {
        super();
        pipelineConf = SpringApplicationContext.getBean(ZkDeployPipelineConf.class);
    }

    @Override
    public String identifier() {
        return Constants.ZK_DEPLOY_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        FlowDeployType deployType = pipelineParameter.getFlowEntity().getDeployType();

        switch (deployType) {
            case CAPACITY_EXPANSION:
                return getExpansionPipelineEvents(pipelineParameter);
            case RESTART_SERVICE:
                return getRestartPipelineEvents(pipelineParameter);
            case OFF_LINE_EVICTION:
                return getOfflinePipelineEvents(pipelineParameter);
            case ITERATION_RELEASE:
                return getIterationPipelineEvents(pipelineParameter);
            default:
                throw new IllegalArgumentException("不支持的部署类型" + deployType);
        }
    }

    private List<UnResolveEvent> getIterationPipelineEvents(PipelineParameter pipelineParameter) {
        List<UnResolveEvent> events = new ArrayList<>();

        UnResolveEvent otherNodeRefreshEvent = new UnResolveEvent();
        otherNodeRefreshEvent.setEventTypeEnum(EventTypeEnum.ZK_NODE_REFRESH_CONFIG);
        otherNodeRefreshEvent.setScope(EventReleaseScope.EVENT);
        otherNodeRefreshEvent.setProjectCode(pipelineConf.getProjectCode());
        otherNodeRefreshEvent.setPipelineCode(pipelineConf.getRefreshConfigPipelineId());
        otherNodeRefreshEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(otherNodeRefreshEvent);

        UnResolveEvent clusterCheckEvent = new UnResolveEvent();
        clusterCheckEvent.setEventTypeEnum(EventTypeEnum.ZK_CLUSTER_STATUS_CHECK);
        clusterCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(clusterCheckEvent);

        return events;
    }

    private List<UnResolveEvent> getRestartPipelineEvents(PipelineParameter pipelineParameter) {
        List<UnResolveEvent> events = new ArrayList<>();
        final UnResolveEvent restartPipelineEvent = new UnResolveEvent();
        restartPipelineEvent.setEventTypeEnum(EventTypeEnum.ZK_RESTART);
        restartPipelineEvent.setScope(EventReleaseScope.EVENT);
        restartPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        restartPipelineEvent.setPipelineCode(pipelineConf.getRestartPipelineId());
        restartPipelineEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(restartPipelineEvent);

        UnResolveEvent clusterCheckEvent = new UnResolveEvent();
        clusterCheckEvent.setEventTypeEnum(EventTypeEnum.ZK_CLUSTER_STATUS_CHECK);
        clusterCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(clusterCheckEvent);

        return events;
    }

    private List<UnResolveEvent> getOfflinePipelineEvents(PipelineParameter pipelineParameter) {
        List<UnResolveEvent> events = new ArrayList<>();

        UnResolveEvent updateConfEvent = new UnResolveEvent();
        updateConfEvent.setEventTypeEnum(EventTypeEnum.ZK_EVICTION_UPDATE_CONF);
        updateConfEvent.setScope(EventReleaseScope.EVENT);
        events.add(updateConfEvent);

        final UnResolveEvent expansionPipelineEvent = new UnResolveEvent();
        expansionPipelineEvent.setEventTypeEnum(EventTypeEnum.ZK_EVICTION_DEPLOY);
        expansionPipelineEvent.setScope(EventReleaseScope.EVENT);
        expansionPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        expansionPipelineEvent.setPipelineCode(pipelineConf.getEvictionPipelineId());
        expansionPipelineEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(expansionPipelineEvent);

        UnResolveEvent otherNodeRefreshEvent = new UnResolveEvent();
        otherNodeRefreshEvent.setEventTypeEnum(EventTypeEnum.ZK_OTHER_NODE_REFRESH_CONFIG);
        otherNodeRefreshEvent.setScope(EventReleaseScope.EVENT);
        otherNodeRefreshEvent.setProjectCode(pipelineConf.getProjectCode());
        otherNodeRefreshEvent.setPipelineCode(pipelineConf.getRefreshConfigPipelineId());
        otherNodeRefreshEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(otherNodeRefreshEvent);

        UnResolveEvent clusterCheckEvent = new UnResolveEvent();
        clusterCheckEvent.setEventTypeEnum(EventTypeEnum.ZK_CLUSTER_STATUS_CHECK);
        clusterCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(clusterCheckEvent);

        return events;
    }

    private List<UnResolveEvent> getExpansionPipelineEvents(PipelineParameter pipelineParameter) {
        List<UnResolveEvent> events = new ArrayList<>();

        UnResolveEvent updateConf = new UnResolveEvent();
        updateConf.setEventTypeEnum(EventTypeEnum.ZK_EXPANSION_UPDATE_CONF);
        updateConf.setScope(EventReleaseScope.EVENT);
        events.add(updateConf);

        final UnResolveEvent expansionPipelineEvent = new UnResolveEvent();
        expansionPipelineEvent.setEventTypeEnum(EventTypeEnum.ZK_EXPANSION_DEPLOY);
        expansionPipelineEvent.setScope(EventReleaseScope.EVENT);
        expansionPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        expansionPipelineEvent.setPipelineCode(pipelineConf.getExpansionPipelineId());
        expansionPipelineEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(expansionPipelineEvent);


        UnResolveEvent otherNodeRefreshEvent = new UnResolveEvent();
        otherNodeRefreshEvent.setEventTypeEnum(EventTypeEnum.ZK_OTHER_NODE_REFRESH_CONFIG);
        otherNodeRefreshEvent.setScope(EventReleaseScope.EVENT);
        otherNodeRefreshEvent.setProjectCode(pipelineConf.getProjectCode());
        otherNodeRefreshEvent.setPipelineCode(pipelineConf.getRefreshConfigPipelineId());
        otherNodeRefreshEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
        events.add(otherNodeRefreshEvent);

        UnResolveEvent clusterCheckEvent = new UnResolveEvent();
        clusterCheckEvent.setEventTypeEnum(EventTypeEnum.ZK_CLUSTER_STATUS_CHECK);
        clusterCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(clusterCheckEvent);

        return events;
    }
}
