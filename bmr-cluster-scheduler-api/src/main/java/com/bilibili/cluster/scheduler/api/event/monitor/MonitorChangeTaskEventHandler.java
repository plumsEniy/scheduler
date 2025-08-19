package com.bilibili.cluster.scheduler.api.event.monitor;


import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.metric.MetricService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.UpdateMetricDto;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.node.monitor.MonitorNodeParams;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.RetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@Slf4j
@Component
public class MonitorChangeTaskEventHandler extends AbstractTaskEventHandler {

    @Resource
    MetricService metricService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        Long executionNodeId = taskEvent.getExecutionNodeId();
        MonitorNodeParams monitorNodeParams = executionNodePropsService.queryNodePropsByNodeId(executionNodeId, MonitorNodeParams.class);
        if (Objects.isNull(monitorNodeParams)) {
            String msg = String.format("process monitor event of %s, but monitor node params is null", taskEvent.getSummary());
            log.info(msg);
            logPersist(taskEvent, msg);
            return true;
        }

        // 随机等待
        Thread.sleep(taskEvent.getRandom().nextInt(Constants.BLOCKS_THRESHOLD * 1000));

        // first remove
        List<MetricNodeInstance> removeMonitorInstanceList = monitorNodeParams.getRemoveMonitorInstanceList();
        incrementalChangeMonitorObject(taskEvent, removeMonitorInstanceList, monitorNodeParams, false);

        // then add
        List<MetricNodeInstance> addMonitorInstanceList = monitorNodeParams.getAddMonitorInstanceList();
        incrementalChangeMonitorObject(taskEvent, addMonitorInstanceList, monitorNodeParams, true);

        return true;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.MONITOR_OBJECT_CHANGE_EXEC_EVENT;
    }

    private boolean incrementalChangeMonitorObject(TaskEvent taskEvent, List<MetricNodeInstance> instanceList,
                                                   MonitorNodeParams monitorNodeParams, boolean addOrRemove) throws Exception {
        if (CollectionUtils.isEmpty(instanceList)) {
            return true;
        }
        Callable<Boolean> func = () ->
                doIncrementalChangeWithMonitorService(taskEvent, instanceList,
                        monitorNodeParams.getMonitorEnv(), monitorNodeParams.getToken(), monitorNodeParams.getIntegrationId(),
                        addOrRemove);
        return RetryUtils.retryWith(3, 3, func);
    }

    private boolean doIncrementalChangeWithMonitorService(TaskEvent taskEvent, List<MetricNodeInstance> instanceList,
                                                          MetricEnvEnum env, String token, int integrationId, boolean addOrRemove) {
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

}
