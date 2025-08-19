package com.bilibili.cluster.scheduler.api.event.flow;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowAopEventType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;

/**
 * @description:
 * @Date: 2025/5/12 17:04
 * @Author: nizhiqiang
 */
public interface AbstractFlowAopEventHandler {

    /**
     * 创建工作流
     *
     * @param flow
     */
    default boolean createFlow(ExecutionFlowEntity flow) {
        return false;
    }

    /**
     * 工作流开始
     *
     * @param flow
     * @throws Exception
     */
    default boolean startFlow(ExecutionFlowEntity flow) {
        return false;
    }

    /**
     * 放弃工作流，包括取消和审批不通过
     *
     * @param flow
     * @throws Exception
     */
    default boolean giveUpFlow(ExecutionFlowEntity flow) {
        return false;
    }

    /**
     * 任务失败
     *
     * @param flow
     * @throws Exception
     */
    default boolean jobFail(ExecutionFlowEntity flow, ExecutionNodeEntity executionNode, String errorMsg) {
        return false;
    }

    /**
     * 批次任务完成
     *
     * @param flow
     * @throws Exception
     */
    default boolean jobFinish(ExecutionFlowEntity flow, ExecutionNodeEntity executionNode) {
        return false;
    }

    /**
     * 工作流完成
     *
     * @param flow
     * @throws Exception
     */
    default boolean finishFlow(ExecutionFlowEntity flow, FlowStatusEnum beforeStatus) {
        return false;
    }

    FlowAopEventType getEventType();
}
