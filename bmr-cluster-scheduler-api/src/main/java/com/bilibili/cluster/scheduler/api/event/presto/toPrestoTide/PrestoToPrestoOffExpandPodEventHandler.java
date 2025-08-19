package com.bilibili.cluster.scheduler.api.event.presto.toPrestoTide;

import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoDeployEventHandler;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoToPrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @Date: 2025/5/15 17:02
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class PrestoToPrestoOffExpandPodEventHandler extends AbstractTidePrestoDeployEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TO_PRESTO_TIDE_OFF_POD_EXPANSION;
    }

    @Override
    protected Integer getNeedScalePodCount(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams params = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);
        return params.getSinkCurrentPod();
    }

    @Override
    protected Long getComponentId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams params = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);
        return params.getSinkComponentId();
    }

    @Override
    protected Long getClusterId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams params = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);
        return params.getSinkClusterId();
    }

    /**
     * presto节点快速缩容,仅在阶段一执行
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("1")) {
            return true;
        } else {
            return false;
        }
    }
}
