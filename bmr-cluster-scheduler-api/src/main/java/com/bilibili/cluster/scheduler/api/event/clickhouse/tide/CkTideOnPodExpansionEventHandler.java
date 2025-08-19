package com.bilibili.cluster.scheduler.api.event.clickhouse.tide;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @Date: 2024/12/3 15:24
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class CkTideOnPodExpansionEventHandler extends AbstractCkTideDeployEventHandler {

    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.CK_TIDE_ON_POD_EXPANSION;
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
    List<Integer> getShardAllocationList(TaskEvent taskEvent, CkTideExtFlowParams ckTideExtFlowParams, Long configVersionId) {
        int currentPod = ckTideExtFlowParams.getCurrentPod();
        List<Integer> shardAllocationList = Arrays.asList(currentPod);
        logPersist(taskEvent, String.format("需要扩容到%s,扩容列表为%s", currentPod, JSONUtil.toJsonStr(shardAllocationList)));
        return shardAllocationList;
    }
}
