package com.bilibili.cluster.scheduler.api.event.presto.tide;

import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoDeployEventHandler;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @Date: 2024/12/3 15:24
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class PrestoTideOnPodExpansionEventHandler extends AbstractTidePrestoDeployEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_ON_POD_EXPANSION;
    }

    @Override
    protected boolean initZeroTolerance(TaskEvent taskEvent) {
        return true;
    }

    /**
     * presto节点扩容,仅在阶段2执行
     *
     * @param taskEvent
     * @return
     */
    @Override
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {
        final ExecutionNodeEntity executionNode = taskEvent.getExecutionNode();
        final String execStage = executionNode.getExecStage();
        if (execStage.equalsIgnoreCase("2")) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    protected Integer getNeedScalePodCount(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoTideExtFlowParams params = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoTideExtFlowParams.class);
        return params.getCurrentPod();
    }
}
