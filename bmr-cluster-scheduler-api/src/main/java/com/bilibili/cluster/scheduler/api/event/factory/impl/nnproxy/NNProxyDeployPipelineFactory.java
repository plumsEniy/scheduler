package com.bilibili.cluster.scheduler.api.event.factory.impl.nnproxy;

import cn.hutool.json.JSONUtil;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.conf.NNProxyDeployPipelineConf;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.SubDeployType;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
public class NNProxyDeployPipelineFactory extends AbstractPipelineFactory {

    private ExecutionFlowPropsService flowPropsService;

    private NNProxyDeployPipelineConf pipelineConf;

    public NNProxyDeployPipelineFactory() {
        super();
        flowPropsService = SpringApplicationContext.getBean(ExecutionFlowPropsService.class);
        pipelineConf = SpringApplicationContext.getBean(NNProxyDeployPipelineConf.class);
        log.info("NNProxyDeployPipelineFactory pipelineConf is {}", pipelineConf);
    }

    @Override
    public String identifier() {
        return Constants.NN_PROXY_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        final ExecutionFlowEntity flowEntity = pipelineParameter.getFlowEntity();

        final DeployOneFlowReq req = pipelineParameter.getReq();
        NNProxyDeployFlowExtParams deployFlowExtParams;
        if (Objects.isNull(req)) {
            deployFlowExtParams = flowPropsService.getFlowExtParamsByCache(flowEntity.getId(), NNProxyDeployFlowExtParams.class);
        } else {
            final String extParams = req.getExtParams();
            deployFlowExtParams = JSONUtil.toBean(extParams, NNProxyDeployFlowExtParams.class);
        }
        Preconditions.checkNotNull(deployFlowExtParams, "NNProxyDeployFlowExtParams is null");
        final SubDeployType subDeployType = deployFlowExtParams.getSubDeployType();

        switch (subDeployType) {
            case CAPACITY_EXPANSION:
                return handleNNProxyExpansion();
            case ITERATION_RELEASE:
                return handleNNProxyIteration();
            default:
                throw new IllegalArgumentException("un-support NNProxy deploy type: " + subDeployType);
        }
    }

    private List<UnResolveEvent> handleNNProxyExpansion() {
        List<UnResolveEvent> events = new ArrayList<>();
        // 扩容主流程
        final UnResolveEvent expansionPipelineEvent = new UnResolveEvent();
        expansionPipelineEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_EXPANSION_PIPELINE_EVENT);
        expansionPipelineEvent.setScope(EventReleaseScope.EVENT);
        expansionPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        expansionPipelineEvent.setPipelineCode(pipelineConf.getExpansionPipelineId());
        expansionPipelineEvent.setFailureStrategy(DolpFailureStrategy.END);
        events.add(expansionPipelineEvent);

        // 扩容状态更新
        final UnResolveEvent updateStateEvent = new UnResolveEvent();
        updateStateEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_EXPANSION_UPDATE_STATE);
        updateStateEvent.setScope(EventReleaseScope.EVENT);
        events.add(updateStateEvent);

        return events;
    }

    private List<UnResolveEvent> handleNNProxyIteration() {
        List<UnResolveEvent> events = new ArrayList<>();

        // 迭代预检查，主要记录上个版本信息（回滚使用）
        final UnResolveEvent preCheckEvent = new UnResolveEvent();
        preCheckEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_ITERATION_PRE_CHECK);
        preCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(preCheckEvent);

        // 迭代主流程
        final UnResolveEvent iterationPipelineEvent = new UnResolveEvent();
        iterationPipelineEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_ITERATION_PIPELINE_EVENT);
        iterationPipelineEvent.setScope(EventReleaseScope.EVENT);
        iterationPipelineEvent.setProjectCode(pipelineConf.getProjectCode());
        iterationPipelineEvent.setPipelineCode(pipelineConf.getIterationPipelineId());
        iterationPipelineEvent.setFailureStrategy(DolpFailureStrategy.END);
        events.add(iterationPipelineEvent);

        // 迭代指标检查
        final UnResolveEvent metricsCheckEvent = new UnResolveEvent();
        metricsCheckEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_ITERATION_METRICS_CHECK);
        metricsCheckEvent.setScope(EventReleaseScope.EVENT);
        events.add(metricsCheckEvent);

        // 迭代状态更新
        final UnResolveEvent updateStateEvent = new UnResolveEvent();
        updateStateEvent.setEventTypeEnum(EventTypeEnum.NN_PROXY_ITERATION_UPDATE_STATE);
        updateStateEvent.setScope(EventReleaseScope.EVENT);
        events.add(updateStateEvent);

        return events;
    }

}
