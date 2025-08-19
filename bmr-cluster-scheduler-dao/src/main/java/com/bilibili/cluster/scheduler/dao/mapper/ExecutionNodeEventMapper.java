package com.bilibili.cluster.scheduler.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
public interface ExecutionNodeEventMapper extends BaseMapper<ExecutionNodeEventEntity> {

    List<ExecutionNodeEventEntity> queryByExecutionNodeIdAndFlowId(@Param("flowId") Long flowId, @Param("executionNodeId") Long executionNodeId);

    void updateEventStatus(@Param("taskEvent") TaskEvent taskEvent);

    /**
     * 根据jobId 更新event 状态
     */
    void updateEventStatusByExecutionNodeId(@Param("executionNodeId") Long executionNodeId, @Param("eventStatus") EventStatusEnum eventStatus);

    void updateEventStatusWithoutSuccessByExecutionNodeId(@Param("executionNodeId") Long executionNodeId, @Param("eventStatus") EventStatusEnum eventStatus);

    /**
     * 批量插入
     *
     * @param executionNodeEventEntityList
     * @return
     */
    boolean batchInsert(@Param("executionNodeEventEntityList") List<ExecutionNodeEventEntity> executionNodeEventEntityList);

}
