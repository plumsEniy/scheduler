package com.bilibili.cluster.scheduler.dao.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.cluster.scheduler.common.dto.node.BaseExecutionNodeDTO;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
public interface ExecutionNodeMapper extends BaseMapper<ExecutionNodeEntity> {

    /**
     * 根据状态和flowId 查询
     */
    List<ExecutionNodeEntity> queryByFlowIdAndNodeStatus(@Param("flowId") Long flowId, @Param("batchId") Integer batchId, @Param("nodeStatus") List<NodeExecuteStatusEnum> nodeStatus);

    /**
     * 根据状态和flowId 查询
     */
    List<ExecutionNodeEntity> getByFlowIdAndNodeStatus(@Param("flowId") Long flowId, @Param("nodeStatus") List<NodeExecuteStatusEnum> nodeStatus);


    /**
     * 更新node状态, 根据BatchId, flowId更新
     */
    public void updateNodeStatusByFlowIdAndBatchId(@Param("flowId") Long flowId, @Param("batchId") Integer batchId, @Param("nodeExecuteStatus") NodeExecuteStatusEnum nodeExecuteStatus);

    /**
     * 根据状态查询任务
     */
    List<ExecutionNodeEntity> findExecuteNodeByNodeStatus(@Param("nodeStatus") List<NodeExecuteStatusEnum> nodeStatus);

    /**
     * 修改node状态
     */
    void updateNodeStatusById(@Param("id") Long id, @Param("nodeExecuteStatus") NodeExecuteStatusEnum nodeExecuteStatus);

    /**
     * 修改状态
     */
    public void updateNodeStatusByFlowIdAndNodeStatus(@Param("flowId") Long flowId, @Param("originNodeExecuteStatus") NodeExecuteStatusEnum originNodeExecuteStatus, @Param("targetNodeExecuteStatus") NodeExecuteStatusEnum targetNodeExecuteStatus);

    /**
     * 批量插入接口
     *
     * @param executionNodeEntityList
     * @return
     */
    boolean batchInsert(@Param("executionNodeEntityList") List<ExecutionNodeEntity> executionNodeEntityList);

    IPage<BaseExecutionNodeDTO> queryNodePage(Page<ExecutionNodeEntity> page, @Param(Constants.WRAPPER) LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper);


    /**
     * 更新任务开始时间. 或者结束时间
     */
    public void updateNodeStartTimeOrEndTime(@Param("id") Long id, @Param("instanceId") long instanceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);


    List<ExecutionNodeSummary> selectExecutionNodeSummary(@Param("flowId") Long flowId);

}
