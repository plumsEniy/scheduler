package com.bilibili.cluster.scheduler.api.event.factory.impl.spark;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SparkVersionDeployEventPipelineFactory extends AbstractPipelineFactory {

    private ExecutionFlowPropsService flowPropsService;

    public SparkVersionDeployEventPipelineFactory() {
        super();
        flowPropsService = SpringApplicationContext.getBean(ExecutionFlowPropsService.class);
    }

    @Override
    public String identifier() {
        return Constants.SPARK_DEPLOY_PIPELINE_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {

        final UnResolveEvent sparkDeployPreCheckEvent = new UnResolveEvent();
        sparkDeployPreCheckEvent.setScope(EventReleaseScope.EVENT);
        sparkDeployPreCheckEvent.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_DEPLOY_PRE_CHECK);

        final UnResolveEvent sparkDeployDqcEvent = new UnResolveEvent();
        sparkDeployDqcEvent.setScope(EventReleaseScope.EVENT);
        sparkDeployDqcEvent.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_DEPLOY_DQC);

        final UnResolveEvent sparkDeployEvent = new UnResolveEvent();
        sparkDeployEvent.setScope(EventReleaseScope.EVENT);
        sparkDeployEvent.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_DEPLOY_EXEC_EVENT);

        final UnResolveEvent sparkDeployPostCheckEvent = new UnResolveEvent();
        sparkDeployPostCheckEvent.setScope(EventReleaseScope.EVENT);
        sparkDeployPostCheckEvent.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_DEPLOY_POST_CHECK);

        final UnResolveEvent sparkDeployStageCheckEvent = new UnResolveEvent();
        sparkDeployStageCheckEvent.setScope(EventReleaseScope.STAGE);
        sparkDeployStageCheckEvent.setEventTypeEnum(EventTypeEnum.SPARK_VERSION_DEPLOY_STAGE_CHECK);

        final ExecutionFlowEntity flowEntity = pipelineParameter.getFlowEntity();
        final FlowDeployType deployType = flowEntity.getDeployType();

        final DeployOneFlowReq req = pipelineParameter.getReq();
        SparkDeployFlowExtParams deployFlowExtParams;
        if (Objects.isNull(req)) {
            deployFlowExtParams = flowPropsService.getFlowExtParamsByCache(flowEntity.getId(), SparkDeployFlowExtParams.class);
        } else {
            final String extParams = req.getExtParams();
            deployFlowExtParams = JSONUtil.toBean(extParams, SparkDeployFlowExtParams.class);
        }
        Preconditions.checkNotNull(deployFlowExtParams, "SparkDeployFlowExtParams is null");

        switch (deployType) {
            case SPARK_DEPLOY:
                if (SparkDeployType.NORMAL == deployFlowExtParams.getSparkDeployType()) {
                    return Arrays.asList(sparkDeployPreCheckEvent, sparkDeployEvent, sparkDeployStageCheckEvent);
                }
                // return Arrays.asList(sparkDeployPreCheckEvent, sparkDeployDqcEvent, sparkDeployEvent, sparkDeployPostCheckEvent);
            default:
                return Arrays.asList(sparkDeployPreCheckEvent, sparkDeployEvent);
        }

    }
}
