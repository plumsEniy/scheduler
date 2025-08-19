package com.bilibili.cluster.scheduler.api.service.flow.status;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;

public interface ExecuteFlowStatusProcess {
    /**
     * 修改flow工作流状态
     */
    void updateFlowStatus();

    /**
     * 更新具体flow状态
     *
     * @param executionFlowEntity
     */
    void updateOneFlowStatus(ExecutionFlowEntity executionFlowEntity);

    /**
     * 修改flow工作流状态
     */
    void updateFlowStatus(Long flowId);

    /**
     * 修改job执行状态
     */
    void updateNodeExecuteStatus();

    /**
     * 更新具体节点的执行状态
     *
     * @param executionNodeEntity
     */
    void updateOneNodeExecuteStatus(ExecutionNodeEntity executionNodeEntity);

    /**
     * 修改job执行状态
     */
    void updateNodeExecuteStatus(Long id);

}
