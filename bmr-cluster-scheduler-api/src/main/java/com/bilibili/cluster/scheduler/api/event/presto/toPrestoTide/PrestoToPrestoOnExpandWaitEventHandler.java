package com.bilibili.cluster.scheduler.api.event.presto.toPrestoTide;

import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoWaitEventHandler;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoToPrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: presto上线等待事件
 * @Date: 2024/12/4 20:01
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class PrestoToPrestoOnExpandWaitEventHandler extends AbstractTidePrestoWaitEventHandler {
    @Override
    protected boolean checkPodCount(TaskEvent taskEvent, List<PodInfo> workPodList) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);

        int remainPod = prestoTideExtFlowParams.getSourceRemainPod();
        int currentPod = prestoTideExtFlowParams.getSourceCurrentPod();

        int needPodCount = (int) Math.floor((currentPod - remainPod) * 0.9 + remainPod);
        logPersist(taskEvent, String.format("当前running的work容器数量为%s,所需容器数量为%s", workPodList.size(), needPodCount));
        if (workPodList.size() >= needPodCount) {
            return true;
        }
        return false;
    }

    protected Long getComponentId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);
        return prestoTideExtFlowParams.getSinkComponentId();
    }

    protected Long getClusterId(TaskEvent taskEvent) {
        Long flowId = taskEvent.getFlowId();
        PrestoToPrestoTideExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoToPrestoTideExtFlowParams.class);
        return prestoTideExtFlowParams.getSourceClusterId();
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TO_PRESTO_TIDE_ON_WAIT_EXPANSION_POD;
    }

    /**
     * presto节点缩容状态检查,仅在阶段2执行
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
}
