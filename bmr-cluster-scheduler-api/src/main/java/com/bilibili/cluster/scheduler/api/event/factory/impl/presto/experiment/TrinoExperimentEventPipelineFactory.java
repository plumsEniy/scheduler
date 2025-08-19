package com.bilibili.cluster.scheduler.api.event.factory.impl.presto.experiment;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoClusterInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoExperimentExtFlowParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrinoExperimentEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.TRINO_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        long flowId = pipelineParameter.getFlowEntity().getId();
        final DeployOneFlowReq req = pipelineParameter.getReq();
        TrinoExperimentExtFlowParams extFlowParams;
        if (Objects.isNull(req)) {
            final ExecutionFlowPropsService flowPropsService = SpringApplicationContext.getBean(ExecutionFlowPropsService.class);
            extFlowParams = flowPropsService.getFlowExtParamsByCache(flowId, TrinoExperimentExtFlowParams.class);
        } else {
            final String extParams = req.getExtParams();
            extFlowParams = JSONUtil.toBean(extParams, TrinoExperimentExtFlowParams.class);
        }
        boolean requireRebuildCluster = false;
        final String aRunTimeConf = extFlowParams.getARunTimeConf();
        final TrinoClusterInfo aClusterInfo = JSONUtil.toBean(aRunTimeConf, TrinoClusterInfo.class);

        requireRebuildCluster |= aClusterInfo.isRebuildCluster();
        if (extFlowParams.getExperimentType().equals(ExperimentType.COMPARATIVE_TASK)) {
            final String bRunTimeConf = extFlowParams.getBRunTimeConf();
            final TrinoClusterInfo bClusterInfo = JSONUtil.toBean(bRunTimeConf, TrinoClusterInfo.class);
            requireRebuildCluster |= bClusterInfo.isRebuildCluster();
        }

        List<UnResolveEvent> events = new ArrayList<>();

        // Stage-1
        if (requireRebuildCluster) {
            // 停止集群
            final UnResolveEvent deactivateEvent = new UnResolveEvent();
            deactivateEvent.setEventTypeEnum(EventTypeEnum.PRESTO_ITERATION_DEACTIVATE_CLUSTER);
            deactivateEvent.setScope(EventReleaseScope.EVENT);
            events.add(deactivateEvent);

            // 删除集群
            final UnResolveEvent deleteEvent = new UnResolveEvent();
            deleteEvent.setEventTypeEnum(EventTypeEnum.PRESTO_ITERATION_DELETED_CLUSTER);
            deleteEvent.setScope(EventReleaseScope.EVENT);
            events.add(deleteEvent);

            // 部署集群
            final UnResolveEvent deployEvent = new UnResolveEvent();
            deployEvent.setEventTypeEnum(EventTypeEnum.PRESTO_ITERATION_DEPLOY_CLUSTER);
            deployEvent.setScope(EventReleaseScope.EVENT);
            events.add(deployEvent);

            // 检查集群
            final UnResolveEvent checkEvent = new UnResolveEvent();
            checkEvent.setEventTypeEnum(EventTypeEnum.PRESTO_ITERATION_GLOBAL_CHECK_CLUSTER);
            checkEvent.setScope(EventReleaseScope.EVENT);
            events.add(checkEvent);

            // 启用集群
            final UnResolveEvent activateEvent = new UnResolveEvent();
            activateEvent.setEventTypeEnum(EventTypeEnum.PRESTO_ITERATION_ACTIVE_CLUSTER);
            activateEvent.setScope(EventReleaseScope.EVENT);
            events.add(activateEvent);
        }

        // Stage-2
        final UnResolveEvent createExperimentEvent = new UnResolveEvent();
        createExperimentEvent.setScope(EventReleaseScope.EVENT);
        createExperimentEvent.setEventTypeEnum(EventTypeEnum.TRINO_EXPERIMENT_CREATE_EXEC_EVENT);

        final UnResolveEvent queryExperimentResultEvent = new UnResolveEvent();
        queryExperimentResultEvent.setScope(EventReleaseScope.EVENT);
        queryExperimentResultEvent.setEventTypeEnum(EventTypeEnum.TRINO_EXPERIMENT_QUERY_EXEC_EVENT);

        events.add(createExperimentEvent);
        events.add(queryExperimentResultEvent);
        return events;
    }
}
