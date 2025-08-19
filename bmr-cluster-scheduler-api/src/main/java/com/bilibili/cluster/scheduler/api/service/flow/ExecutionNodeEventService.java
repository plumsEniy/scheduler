package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
public interface ExecutionNodeEventService extends IService<ExecutionNodeEventEntity> {

    public List<ExecutionNodeEventEntity> queryByExecutionNodeIdAndFlowId(Long flowId, Long executionNodeId);

    public void updateEventStatus(TaskEvent taskEvent);

    void updateGlobalEventStatus(TaskEvent taskEvent);

    void updateBatchEventStatus(TaskEvent taskEvent);

    /**
     * 批量插入
     *
     * @param executionNodeEventEntityList
     * @return
     */
    boolean batchInsert(List<ExecutionNodeEventEntity> executionNodeEventEntityList);

    /**
     * 根据jobid查询event列表，会根据任务id和事件执行顺序排序
     *
     * @param executionNodeIdList
     */
    List<ExecutionNodeEventEntity> queryEventListByNodeIds(Collection<Long> executionNodeIdList);

    /**
     * 查询event日志
     *
     * @param nodeEventId
     * @return
     */
    ResponseResult queryNodeEventLog(Long nodeEventId);

    /**
     * 根据jobid查询event列表
     *
     * @param nodeId
     * @return
     */
    ResponseResult queryNodeEventListByExecutionNodeId(Long nodeId);

    List<ExecutionNodeEventEntity> queryNodeEventListByNodeId(Long nodeId);

    /**
     * 根据job id 批量修改event状态
     */
    public void batchUpdateEventStatus(List<Long> nodeIdList, EventStatusEnum eventStatus);

    public void batchUpdateEventStatusWithoutSuccess(List<Long> nodeIdList, EventStatusEnum eventStatus);

    /**
     * 批量保存job事件
     *
     * @param flowId
     * @param executionNodeList
     * @param eventTypeEnumList
     */
    void batchSaveExecutionNodeEvent(Long flowId, List<ExecutionNodeEntity> executionNodeList, List<ResolvedEvent> eventTypeEnumList);

    /**
     * 根据工作流id和执行顺序查询事件
     *
     * @param flowId
     * @param executeOrder
     * @return
     */
    List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrder(Long flowId, Integer executeOrder);

    /**
     * 根据工作流id,执行顺序和批次id查询事件
     *
     * @param flowId
     * @param batchId
     * @param executeOrder
     * @return
     */
    List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrderBatchId(Long flowId, Integer batchId, Integer executeOrder);

    /**
     * 全局更新实例id
     *
     * @param flowId
     * @param schedId
     */
    void updateGlobalSchedId(Long flowId, Integer executeOrder, String schedId);

    /**
     * 更新单个实例的schedid
     */
    void updateSchedId(Long eventId, String schedId);

    /**
     * 更新批次的schediid
     *
     * @param flowId
     * @param executeOrder
     * @param batchId
     * @param schedId
     */
    void updateBatchSchedId(Long flowId, Integer executeOrder, Integer batchId, String schedId);

    /**
     * 更新批次event所在的instanceId
     * @param flowId
     * @param instanceId
     * @param nodeIdList
     */
    void updateBatchNodeEventInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList);

    /**
     * 查询节点 [1, maxOrder] 范围的event列表
     * @param flowId
     * @param instanceId
     * @param nodeId
     * @param maxExecuteOrder
     * @return
     */
    List<ExecutionNodeEventEntity> queryPrePipelineEvents(long flowId, long instanceId, Long nodeId, int maxExecuteOrder);

    /**
     * 批量更新 dolphin pipeline event 列表
     * * @param flowId
     * @param instanceId
     * @param projectCode
     * @param pipelineCode
     * @param nodeIdList
     * @param schedInInstanceId
     */
    void updateBatchNodeEventSchedId(Long flowId, Long instanceId, String projectCode, String pipelineCode, List<Long> nodeIdList, String schedInInstanceId);

    /**
     * 根据工作流id,所属节点id, 执行顺序NO 查询事件详情
     * @param flowId
     * @param instanceId
     * @param executionNodeId
     * @param executeOrder
     * @return
     */
    ExecutionNodeEventEntity queryCurrentNodeEventByOrderNo(Long flowId, Long instanceId, Long executionNodeId, int executeOrder);

    /**
     * 更新event执行态数据
     * @param eventStatus
     * @param eventEndTime
     * @param jobSetId
     * @param jobTaskId
     */
    void updateEventExecDate(Long flowId, Long instanceId, Long nodeId, String taskCode, String schedulerInstanceId,
            EventStatusEnum eventStatus, LocalDateTime eventEndTime, long jobSetId, long jobTaskId);

    /**
     * 批量重试场景重置event执行状态
     * @param targetJobIdList
     * @param unEventExecute
     */
    void batchUpdateEventStatusAndResetInstanceId(List<Long> targetJobIdList, EventStatusEnum unEventExecute, boolean reExecute);

    /**
     * 查询指定顺序，满足状态条件的event列表
     * @param flowId
     * @param instanceId
     * @param executeOrder
     * @param eventStatusList
     * @return
     */
    List<ExecutionNodeEventEntity> queryNodeEventListByStatusList(Long flowId, Long instanceId, int executeOrder, List<EventStatusEnum> eventStatusList);


    void updateBatchEventStatus(TaskEvent taskEvent, List<Long> nodeIdList);


    void updateBatchEventLogId(Long flowId, Long instanceId, Integer executeOrder, List<Long> nodeIdList, long logId);

    ExecutionNodeEventEntity queryOneNodeEvent(ExecutionNodeEventEntity queryEventDo);

    List<ExecutionNodeEventEntity> queryNodeEventList(ExecutionNodeEventEntity queryEventDo);

    /**
     * 更新定制 dolphin-scheduler pipeline code and instanceId
     * @param flowId
     * @param instanceId
     * @param nodeIdList
     * @param projectCode
     * @param pipelineCode
     * @param schedInInstanceId
     */
    void updateBatchNodeEventSchedIdAndPipelineCode(Long flowId, Long instanceId, List<Long> nodeIdList, String projectCode, String pipelineCode, String schedInInstanceId);

    /**
     * 根据工作流id,执行顺序和实例Id查询事件
     *
     * @param flowId
     * @param instanceId
     * @param executeOrder
     * @return
     */
    List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrderInstanceId(Long flowId, Long instanceId, Integer executeOrder);

    /**
     * 批量更新event状态
     * @param eventIdList
     * @param eventSkipped
     */
    void updateBatchEventStatusByIdList(Long flowId, List<Long> eventIdList, EventStatusEnum eventSkipped);

    /**
     * 更新event执行状态，require EventId
     * @param flowId
     * @param instanceId
     * @param eventId
     * @param eventStatus
     * @param eventEndTime
     * @param jobSetId
     * @param jobTaskId
     * @return
     */
    boolean updateEventExecDate(Long flowId, Long instanceId, Long eventId,
                                EventStatusEnum eventStatus, LocalDateTime eventEndTime, long jobSetId, long jobTaskId);

}
