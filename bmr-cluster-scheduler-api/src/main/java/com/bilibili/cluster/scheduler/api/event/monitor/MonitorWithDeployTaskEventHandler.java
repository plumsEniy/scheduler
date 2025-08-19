package com.bilibili.cluster.scheduler.api.event.monitor;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.metric.MetricService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataMonitorConf;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model.ComponentHostRelationModel;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.req.QueryComponentHostPageReq;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.UpdateMetricDto;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.MonitorConfUtil;
import com.bilibili.cluster.scheduler.common.utils.RetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
public class MonitorWithDeployTaskEventHandler extends AbstractTaskEventHandler {

    @Resource
    MetricService metricService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        FlowDeployType deployType = taskEvent.getDeployType();
        boolean incrementalAdd = false, incrementalRemove = false;
        switch (deployType) {
            case CAPACITY_EXPANSION:
            case TIDE_ONLINE:
                incrementalAdd = true;
                break;
            case OFF_LINE_EVICTION:
            case TIDE_OFFLINE:
                incrementalRemove = true;
               break;
            default:
                return true;
        }

        if (incrementalAdd) {
            assembleMonitorDtoList(taskEvent, true);
        }

        if (incrementalRemove) {
            assembleMonitorDtoList(taskEvent, false);
        }
        return true;
    }

    private void assembleMonitorDtoList(TaskEvent taskEvent, boolean addOrRemove) throws Exception {
        ExecutionFlowEntity flowEntity = executionFlowService.getById(taskEvent.getFlowId());
        List<MetadataMonitorConf> metadataMonitorConfList = globalService.getBmrMetadataService().queryMonitorConfList(
                flowEntity.getClusterId(), flowEntity.getComponentId());
        if (CollectionUtils.isEmpty(metadataMonitorConfList)) {
            logPersist(taskEvent, "metadataMonitorConfList is blank, skip....");
            return;
        }

        String nodeName = taskEvent.getExecutionNode().getNodeName();
        for (MetadataMonitorConf metadataMonitorConf : metadataMonitorConfList) {
            MetricConfInfo monitorComponent = metadataMonitorConf.getMonitorComponent();
            QueryComponentHostPageReq queryComponentHostReq = new QueryComponentHostPageReq();
            queryComponentHostReq.setClusterId(monitorComponent.getClusterId());
            queryComponentHostReq.setComponentId(monitorComponent.getComponentId());
            queryComponentHostReq.setHostNameList(Arrays.asList(nodeName));

            List<ComponentHostRelationModel> componentHostRelationModels = globalService.getBmrResourceV2Service()
                    .queryComponentHostList(queryComponentHostReq);
            if (CollectionUtils.isEmpty(componentHostRelationModels)) {
                logPersist(taskEvent, "componentHostRelationModels is blank, skip....");
                continue;
            }

            List<MetricNodeInstance> metricNodeInstanceList = MonitorConfUtil.getNodeInstanceList(
                    componentHostRelationModels, metadataMonitorConf.getMonitorComponent());
            if (CollectionUtils.isEmpty(metricNodeInstanceList)) {
                logPersist(taskEvent, "monitorNodeInstanceList is blank, skip....");
                continue;
            }

            // 随机等待
            Thread.sleep(taskEvent.getRandom().nextInt(Constants.BLOCKS_THRESHOLD * 1000));

            Integer integrationId = metadataMonitorConf.getIntegrationId();
            incrementalChangeMonitorObject(taskEvent, metricNodeInstanceList, monitorComponent.getEnvType(),
                    metadataMonitorConf.getToken(), integrationId, addOrRemove);
        }
    }

    private boolean incrementalChangeMonitorObject(TaskEvent taskEvent, List<MetricNodeInstance> instanceList,
                                                   MetricEnvEnum env, String token, int integrationId,
                                                   boolean addOrRemove) throws Exception {
        if (CollectionUtils.isEmpty(instanceList)) {
            return true;
        }
        Callable<Boolean> func = () ->
                doIncrementalChangeWithMonitorService(taskEvent, instanceList, env, token, integrationId, addOrRemove);
        return RetryUtils.retryWith(3, 3, func);
    }

    private boolean doIncrementalChangeWithMonitorService(TaskEvent taskEvent, List<MetricNodeInstance> instanceList,
                                                          MetricEnvEnum env, String token, int integrationId,
                                                          boolean addOrRemove) {
        String msg = String.format("will incremental %s monitor object with: \n %s",
                addOrRemove ? "add" : "remove", JSONUtil.toJsonStr(instanceList));
        logPersist(taskEvent, msg);
        UpdateMetricDto updateMetricDto = new UpdateMetricDto();
        updateMetricDto.setInstances(instanceList);
        updateMetricDto.setIntegrationId(integrationId);
        try {
            if (addOrRemove) {
                metricService.addMetricInstance(env, updateMetricDto, token);
                msg = "incremental add monitor object finish.";
            } else {
                metricService.delMetricInstance(env, updateMetricDto, token);
                msg = "incremental remove monitor object finish.";
            }
        } catch (Exception e) {
            msg = "invoker monitor service error, case by: " + ExceptionUtils.getStackTrace(e);
            throw  e;
        } finally {
            logPersist(taskEvent, msg);
        }
        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.MONITOR_WITH_DEPLOY_EXEC_EVENT;
    }
}
