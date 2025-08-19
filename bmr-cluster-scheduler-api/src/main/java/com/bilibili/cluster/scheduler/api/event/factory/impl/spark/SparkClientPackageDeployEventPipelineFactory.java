package com.bilibili.cluster.scheduler.api.event.factory.impl.spark;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.UnResolveEvent;
import com.bilibili.cluster.scheduler.api.event.factory.AbstractPipelineFactory;
import com.bilibili.cluster.scheduler.api.event.spark.client.conf.SparkClientPackDeployPipelineInfo;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployType;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventReleaseScope;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class SparkClientPackageDeployEventPipelineFactory extends AbstractPipelineFactory {

    @Override
    public String identifier() {
        return Constants.SPARK_CLIENT_PACKAGE_DEPLOY_FACTORY_IDENTIFY;
    }

    @Override
    public List<UnResolveEvent> analyzer(PipelineParameter pipelineParameter) {
        SparkClientPackDeployPipelineInfo pipelineInfoConf = SpringApplicationContext.getBean(SparkClientPackDeployPipelineInfo.class);
        log.info("sparkClientPackDeployPipelineInfo conf is {}", pipelineInfoConf);

        final DeployOneFlowReq req = pipelineParameter.getReq();
        SparkClientDeployExtParams sparkClientDeployExtParams;
        if (!Objects.isNull(req)) {
            String extParams = req.getExtParams();
            sparkClientDeployExtParams = JSONUtil.toBean(extParams, SparkClientDeployExtParams.class);
        } else {
            final ExecutionFlowPropsService flowPropsService = SpringApplicationContext.getBean(ExecutionFlowPropsService.class);
            final ExecutionFlowEntity flowEntity = pipelineParameter.getFlowEntity();
            final Long flowId = flowEntity.getId();
            sparkClientDeployExtParams = flowPropsService.getFlowExtParamsByCache(flowId, SparkClientDeployExtParams.class);
        }
        Preconditions.checkNotNull(sparkClientDeployExtParams,
                "SparkClientPackageDeployEventPipelineFactory#analyzer pipeline event, but sparkClientDeployExtParams is null");

        final SparkClientDeployType packDeployType = sparkClientDeployExtParams.getPackDeployType();
        List<UnResolveEvent> eventList = new ArrayList<>();
        switch (packDeployType) {
            case ADD_NEWLY_VERSION:
            case ADD_NEWLY_HOSTS:
                final UnResolveEvent sparkClientPackDownloadEvent = new UnResolveEvent();
                sparkClientPackDownloadEvent.setEventTypeEnum(EventTypeEnum.SPARK_CLIENT_PACK_DOWNLOAD_EVENT);
                sparkClientPackDownloadEvent.setScope(EventReleaseScope.BATCH);
                sparkClientPackDownloadEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
                sparkClientPackDownloadEvent.setProjectCode(pipelineInfoConf.getProjectCode());
                sparkClientPackDownloadEvent.setPipelineCode(pipelineInfoConf.getDownloadPipelineId());
                eventList.add(sparkClientPackDownloadEvent);
                break;

            case REMOVE_USELESS_VERSION:
                final UnResolveEvent sparkClientPackRemoveEvent = new UnResolveEvent();
                sparkClientPackRemoveEvent.setEventTypeEnum(EventTypeEnum.SPARK_CLIENT_PACK_REMOVE_EVENT);
                sparkClientPackRemoveEvent.setScope(EventReleaseScope.BATCH);
                sparkClientPackRemoveEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
                sparkClientPackRemoveEvent.setProjectCode(pipelineInfoConf.getProjectCode());
                sparkClientPackRemoveEvent.setPipelineCode(pipelineInfoConf.getRemovePipelineId());
                eventList.add(sparkClientPackRemoveEvent);
                break;

            case REMOVE_USELESS_HOSTS:
                final UnResolveEvent sparkClientPackCleanEvent = new UnResolveEvent();
                sparkClientPackCleanEvent.setEventTypeEnum(EventTypeEnum.SPARK_CLIENT_PACK_CLEAN_EVENT);
                sparkClientPackCleanEvent.setScope(EventReleaseScope.BATCH);
                sparkClientPackCleanEvent.setFailureStrategy(DolpFailureStrategy.CONTINUE);
                sparkClientPackCleanEvent.setProjectCode(pipelineInfoConf.getProjectCode());
                sparkClientPackCleanEvent.setPipelineCode(pipelineInfoConf.getCleanPipelineId());
                eventList.add(sparkClientPackCleanEvent);
                break;
        }
        return eventList;
    }
}
