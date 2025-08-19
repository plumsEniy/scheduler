package com.bilibili.cluster.scheduler.api.event.hdfs.nnproxy.iteration;

import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.single.AbstractSingleDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodeExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description: nnproxy迭代
 * @Date: 2025/4/28 17:43
 * @Author: nizhiqiang
 */
@Component
@Slf4j
public class NNproxyIterationPipelineEventHandler extends AbstractSingleDolphinSchedulerEventHandler {


    @Override
    protected Long getConfigId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams = executionNodePropsService.queryNodePropsByNodeId(taskEvent.getExecutionNodeId(), NNProxyDeployNodeExtParams.class);
        if (isInRollbackStatus(taskEvent)) {
            return nnProxyDeployNodeExtParams.getBeforeConfigId();
        }

        return nnProxyDeployNodeExtParams.getConfigId();
    }

    @Override
    protected Long getPackageId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams = executionNodePropsService.queryNodePropsByNodeId(taskEvent.getExecutionNodeId(), NNProxyDeployNodeExtParams.class);
        if (isInRollbackStatus(taskEvent)) {
            return nnProxyDeployNodeExtParams.getBeforePackageId();
        }
        return nnProxyDeployNodeExtParams.getPackageId();
    }

    @Override
    protected Long getComponentId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        NNProxyDeployNodeExtParams nnProxyDeployNodeExtParams = executionNodePropsService.queryNodePropsByNodeId(taskEvent.getExecutionNodeId(), NNProxyDeployNodeExtParams.class);
        return nnProxyDeployNodeExtParams.getComponentId();
    }

    @Override
    protected boolean skipLogicNode() {
        return true;
    }


    @Override
    public EventTypeEnum getHandleEventType() {
        return EventTypeEnum.NN_PROXY_ITERATION_PIPELINE_EVENT;
    }

}
