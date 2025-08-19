package com.bilibili.cluster.scheduler.api.event.presto.tide;

import com.bilibili.cluster.scheduler.api.event.presto.AbstractTidePrestoWaitEventHandler;
import com.bilibili.cluster.scheduler.common.dto.caster.PodInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
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
public class PrestoTideOnWaitEventHandler extends AbstractTidePrestoWaitEventHandler {

    @Override
    protected boolean checkPodCount(TaskEvent taskEvent,  List<PodInfo> workPodList) {
        Long flowId = taskEvent.getFlowId();
        PrestoTideExtFlowParams prestoTideExtFlowParams = executionFlowPropsService.getFlowExtParamsByCache(flowId, PrestoTideExtFlowParams.class);
        int remainPod = prestoTideExtFlowParams.getRemainPod();
        int currentPod = prestoTideExtFlowParams.getCurrentPod();

        int needPodCount = (int) Math.floor((currentPod - remainPod) * 0.9 + remainPod);
        logPersist(taskEvent, String.format("当前running的work容器数量为%s,所需容器数量为%s", workPodList.size(), needPodCount));
        if (workPodList.size() >= needPodCount) {
            return true;
        }
        return false;
    }

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.PRESTO_TIDE_ON_POD_STATUS_CHECK;
    }

    /**
     * presto节点扩容状态检查,仅在阶段2执行
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
