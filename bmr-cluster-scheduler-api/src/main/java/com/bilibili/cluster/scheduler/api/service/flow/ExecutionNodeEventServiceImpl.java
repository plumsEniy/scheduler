package com.bilibili.cluster.scheduler.api.service.flow;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.service.jobAgent.JobAgentService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomReport;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionNodeEventMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
@Service
public class ExecutionNodeEventServiceImpl extends ServiceImpl<ExecutionNodeEventMapper, ExecutionNodeEventEntity> implements ExecutionNodeEventService {

    @Resource
    private ExecutionNodeEventMapper executionNodeEventMapper;

    @Resource
    private ExecutionLogService executionLogService;

    @Resource
    JobAgentService jobAgentService;

    @Override
    public List<ExecutionNodeEventEntity> queryByExecutionNodeIdAndFlowId(Long flowId, Long executionNodeId) {
        return executionNodeEventMapper.queryByExecutionNodeIdAndFlowId(flowId, executionNodeId);
    }

    @Override
    public void updateEventStatus(TaskEvent taskEvent) {
        executionNodeEventMapper.updateEventStatus(taskEvent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGlobalEventStatus(TaskEvent taskEvent) {
//        更新所有任务的状态
        LambdaUpdateWrapper<ExecutionNodeEventEntity> globalUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        globalUpdateWrapper.eq(ExecutionNodeEventEntity::getFlowId, taskEvent.getFlowId())
                .eq(ExecutionNodeEventEntity::getExecuteOrder, taskEvent.getExecuteOrder())
                .set(taskEvent.getEventStatus() != null, ExecutionNodeEventEntity::getEventStatus, taskEvent.getEventStatus());

        update(globalUpdateWrapper);
//        更新单个任务的开始和结束时间

        LambdaUpdateWrapper<ExecutionNodeEventEntity> singleUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        singleUpdateWrapper.eq(ExecutionNodeEventEntity::getId, taskEvent.getEventId())
                .set(StringUtils.isEmpty(taskEvent.getHostName()), ExecutionNodeEventEntity::getHostName, taskEvent.getHostName())
                .set(taskEvent.getStartTime() != null, ExecutionNodeEventEntity::getStartTime, taskEvent.getStartTime())
                .set(taskEvent.getEndTime() != null, ExecutionNodeEventEntity::getEndTime, taskEvent.getEndTime());
        update(singleUpdateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchEventStatus(TaskEvent taskEvent) {
        // 更新批次任务的状态
        LambdaUpdateWrapper<ExecutionNodeEventEntity> globalUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        globalUpdateWrapper.eq(ExecutionNodeEventEntity::getFlowId, taskEvent.getFlowId())
                .eq(ExecutionNodeEventEntity::getExecuteOrder, taskEvent.getExecuteOrder())
                .eq(ExecutionNodeEventEntity::getBatchId, taskEvent.getBatchId())
                .set(taskEvent.getEventStatus() != null, ExecutionNodeEventEntity::getEventStatus, taskEvent.getEventStatus());

        update(globalUpdateWrapper);

        // 更新单个任务的开始和结束时间
        LambdaUpdateWrapper<ExecutionNodeEventEntity> singleUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        singleUpdateWrapper.eq(ExecutionNodeEventEntity::getId, taskEvent.getEventId())
                .set(StringUtils.isEmpty(taskEvent.getHostName()), ExecutionNodeEventEntity::getHostName, taskEvent.getHostName())
                .set(taskEvent.getStartTime() != null, ExecutionNodeEventEntity::getStartTime, taskEvent.getStartTime())
                .set(taskEvent.getEndTime() != null, ExecutionNodeEventEntity::getEndTime, taskEvent.getEndTime());
        update(singleUpdateWrapper);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchEventStatus(TaskEvent taskEvent, List<Long> nodeIdList) {
        // 更新批次任务的状态
        LambdaUpdateWrapper<ExecutionNodeEventEntity> batchUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        batchUpdateWrapper.eq(ExecutionNodeEventEntity::getFlowId, taskEvent.getFlowId())
                .eq(ExecutionNodeEventEntity::getInstanceId, taskEvent.getInstanceId())
                .eq(ExecutionNodeEventEntity::getExecuteOrder, taskEvent.getExecuteOrder())
                .in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(taskEvent.getEventStatus() != null, ExecutionNodeEventEntity::getEventStatus, taskEvent.getEventStatus())
                .set(taskEvent.getStartTime() != null, ExecutionNodeEventEntity::getStartTime, taskEvent.getStartTime())
                .set(taskEvent.getEndTime() != null, ExecutionNodeEventEntity::getEndTime, taskEvent.getEndTime());
        update(batchUpdateWrapper);
    }

    @Override
    public void updateBatchEventLogId(Long flowId, Long instanceId, Integer executeOrder, List<Long> nodeIdList, long logId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> batchUpdateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        batchUpdateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder)
                .in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(ExecutionNodeEventEntity::getLogId, logId);

        update(batchUpdateWrapper);
    }

    @Override
    public boolean batchInsert(List<ExecutionNodeEventEntity> executionNodeEventEntityList) {
        return baseMapper.batchInsert(executionNodeEventEntityList);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryEventListByNodeIds(Collection<Long> executionNodeIdList) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.in(ExecutionNodeEventEntity::getExecutionNodeId, executionNodeIdList)
                .orderByAsc(true, ExecutionNodeEventEntity::getExecutionNodeId, ExecutionNodeEventEntity::getExecuteOrder);
        List<ExecutionNodeEventEntity> executionNodeEventEntityList = list(queryWrapper);
        return executionNodeEventEntityList;
    }

    @Override
    public ResponseResult queryNodeEventLog(Long nodeEventId) {
        ExecutionNodeEventEntity eventEntity = executionNodeEventMapper.selectById(nodeEventId);
        EventTypeEnum eventType = eventEntity.getEventType();
//        switch (eventType) {
//            case DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT:
//                Long jobTaskId = eventEntity.getJobTaskId();
//                if (!Objects.isNull(jobTaskId) && jobTaskId > 0) {
//                    TaskAtomReport taskReport = jobAgentService.getTaskReport(jobTaskId);
//                    StringBuilder builder = new StringBuilder();
//                    taskReport.getTaskLog().forEach(taskLog ->
//                        builder.append(taskLog.getMessage()).append(Constants.NEW_LINE));
//                    return ResponseResult.getSuccess(builder.toString());
//                } else {
//                    return ResponseResult.getSuccess("未执行");
//                }
//            default:
//                Long logId = eventEntity.getLogId();
//                if (!Objects.isNull(logId) && logId > 0) {
//                    return ResponseResult.getSuccess(executionLogService.getById(logId).getLogContent());
//                } else {
//                    return ResponseResult.getSuccess(executionLogService.queryLogByExecuteId(nodeEventId, LogTypeEnum.EVENT));
//                }
        StringBuilder builder = new StringBuilder();
        Long logId = eventEntity.getLogId();
        if (!Objects.isNull(logId) && logId > 0) {
            builder.append(executionLogService.getById(logId).getLogContent());
        }
        if (eventType.isDolphinType()) {
            Long jobTaskId = eventEntity.getJobTaskId();
            if (!Objects.isNull(jobTaskId) && jobTaskId > 0) {
                if (builder.length() > 0) {
                    builder.append(Constants.NEW_LINE)
                            .append("-------------[job-agent]----------------")
                            .append(Constants.NEW_LINE);
                }
                TaskAtomReport taskReport = jobAgentService.getTaskReport(jobTaskId);
                taskReport.getTaskLog().forEach(taskLog ->
                    builder.append(taskLog.getMessage()).append(Constants.NEW_LINE));
            }
            return ResponseResult.getSuccess(builder.toString());
        } else {
            if (builder.length() > 0) {
                return ResponseResult.getSuccess(builder.toString());
            } else {
                return ResponseResult.getSuccess(executionLogService.queryLogByExecuteId(nodeEventId, LogTypeEnum.EVENT));
            }
        }
    }

    @Override
    public ResponseResult queryNodeEventListByExecutionNodeId(Long nodeId) {
        return ResponseResult.getSuccess(queryNodeEventListByNodeId(nodeId));
    }

    @Override
    public List<ExecutionNodeEventEntity> queryNodeEventListByNodeId(Long nodeId) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> nodeEventQueryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        nodeEventQueryWrapper.eq(ExecutionNodeEventEntity::getExecutionNodeId, nodeId)
                .orderByAsc(ExecutionNodeEventEntity::getExecuteOrder);
        List<ExecutionNodeEventEntity> executionNodeEventEntityList = list(nodeEventQueryWrapper);
        return executionNodeEventEntityList;
    }

    @Override
    public void batchUpdateEventStatus(List<Long> nodeIdList, EventStatusEnum eventStatus) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(ExecutionNodeEventEntity::getEventStatus, eventStatus);
        update(updateWrapper);
    }

    @Override
    public void batchUpdateEventStatusWithoutSuccess(List<Long> nodeIdList, EventStatusEnum eventStatus) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .ne(ExecutionNodeEventEntity::getEventStatus, EventStatusEnum.SUCCEED_EVENT_EXECUTE)
                .set(ExecutionNodeEventEntity::getEventStatus, eventStatus);
        update(updateWrapper);
    }

    @Override
    public void batchSaveExecutionNodeEvent(Long flowId, List<ExecutionNodeEntity> executionNodeList, List<ResolvedEvent> resolvedEventList) {
        List<ExecutionNodeEventEntity> nodeEventList = new ArrayList<>();
        for (ExecutionNodeEntity executionNode : executionNodeList) {
            Long executionNodeId = executionNode.getId();
            int order = 1;
            for (ResolvedEvent event : resolvedEventList) {
                ExecutionNodeEventEntity executionNodeEvent = new ExecutionNodeEventEntity();
                executionNodeEvent.setEventType(event.getEventTypeEnum());
                executionNodeEvent.setEventName(event.getEventName());
                executionNodeEvent.setExecutionNodeId(executionNodeId);
                executionNodeEvent.setFlowId(flowId);
                executionNodeEvent.setBatchId(executionNode.getBatchId());
                executionNodeEvent.setEventStatus(EventStatusEnum.UN_EVENT_EXECUTE);
                executionNodeEvent.setReleaseScope(event.getScope());
                executionNodeEvent.setExecuteOrder(order++);
                if (event.getEventTypeEnum().isDolphinType()) {
                    executionNodeEvent.setProjectCode(event.getProjectCode());
                    executionNodeEvent.setPipelineCode(event.getPipelineCode());
                    executionNodeEvent.setTaskCode(event.getTaskCode());
                    executionNodeEvent.setFailureStrategy(event.getFailureStrategy().name());
                    executionNodeEvent.setTaskPosType(event.getTaskPosType().name());
                } else {
                    executionNodeEvent.setProjectCode(Constants.EMPTY_STRING);
                    executionNodeEvent.setPipelineCode(Constants.EMPTY_STRING);
                    executionNodeEvent.setTaskCode(Constants.EMPTY_STRING);
                    executionNodeEvent.setFailureStrategy(Constants.EMPTY_STRING);
                    executionNodeEvent.setTaskPosType(Constants.EMPTY_STRING);
                }
                nodeEventList.add(executionNodeEvent);
            }
        }

        List<List<ExecutionNodeEventEntity>> splitNodeEventList = ListUtil.split(nodeEventList, 500);
        for (List<ExecutionNodeEventEntity> split : splitNodeEventList) {
            Assert.isTrue(executionNodeEventMapper.batchInsert(split), "批量插入node event表失败");
        }
    }

    @Override
    public List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrder(Long flowId, Integer executeOrder) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder);
        return list(queryWrapper);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrderBatchId(Long flowId, Integer batchId, Integer executeOrder) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getBatchId, batchId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder);
        return list(queryWrapper);
    }

    @Override
    public void updateGlobalSchedId(Long flowId, Integer executeOrder, String schedInstanceId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, schedInstanceId);
        update(updateWrapper);
    }

    @Override
    public void updateSchedId(Long eventId, String schedId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getId, eventId)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, schedId);
        update(updateWrapper);
    }

    @Override
    public void updateBatchSchedId(Long flowId, Integer executeOrder, Integer batchId, String schedId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getBatchId, batchId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, schedId);
        update(updateWrapper);
    }

    @Override
    public void updateBatchNodeEventInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            log.warn("node id list is null, will not execute update event current instanceId.");
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(ExecutionNodeEventEntity::getInstanceId, instanceId);
        update(updateWrapper);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryPrePipelineEvents(long flowId, long instanceId, Long nodeId, int maxExecuteOrder) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getExecutionNodeId, nodeId)
                .le(ExecutionNodeEventEntity::getExecuteOrder, maxExecuteOrder);
        return list(queryWrapper);
    }

    @Override
    public void updateBatchNodeEventSchedId(Long flowId, Long instanceId, String projectCode, String pipelineCode, List<Long> nodeIdList, String schedInInstanceId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getProjectCode, projectCode)
                .eq(ExecutionNodeEventEntity::getPipelineCode, pipelineCode)
                .in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, schedInInstanceId);
        update(updateWrapper);
    }

    @Override
    public ExecutionNodeEventEntity queryCurrentNodeEventByOrderNo(Long flowId, Long instanceId, Long executionNodeId, int executeOrder) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getExecutionNodeId, executionNodeId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder);
        return getOne(queryWrapper);
    }

    @Override
    public void updateEventExecDate(Long flowId, Long instanceId, Long nodeId, String taskCode, String schedulerInstanceId,
            EventStatusEnum eventStatus, LocalDateTime eventEndTime, long jobSetId, long jobTaskId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                // nodeId 可能为空
                .eq(Objects.nonNull(nodeId), ExecutionNodeEventEntity::getExecutionNodeId, nodeId)
                .eq(ExecutionNodeEventEntity::getTaskCode, taskCode)
                .eq(!StringUtils.isBlank(schedulerInstanceId), ExecutionNodeEventEntity::getSchedInstanceId, schedulerInstanceId)
                .set(Objects.nonNull(eventStatus), ExecutionNodeEventEntity::getEventStatus, eventStatus)
                .set(Objects.nonNull(eventEndTime), ExecutionNodeEventEntity::getEndTime, eventEndTime)
                .set(jobSetId > 0, ExecutionNodeEventEntity::getJobTaskSetId, jobSetId)
                .set(jobTaskId > 0, ExecutionNodeEventEntity::getJobTaskId, jobTaskId);
        update(updateWrapper);
    }

    @Override
    public void batchUpdateEventStatusAndResetInstanceId(List<Long> nodeIdList, EventStatusEnum eventStatus, boolean reExecutePipeline) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .notIn(!reExecutePipeline, ExecutionNodeEventEntity::getEventStatus,
                        Arrays.asList(EventStatusEnum.SUCCEED_EVENT_EXECUTE, EventStatusEnum.EVENT_SKIPPED))
                .set(ExecutionNodeEventEntity::getEventStatus, eventStatus)
                .set(ExecutionNodeEventEntity::getInstanceId, 0l)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, Constants.EMPTY_STRING)
                .set(ExecutionNodeEventEntity::getJobTaskSetId, 0l)
                .set(ExecutionNodeEventEntity::getJobTaskId, 0l);
        update(updateWrapper);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryNodeEventListByStatusList(Long flowId, Long instanceId, int executeOrder, List<EventStatusEnum> eventStatusList) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder)
                .in(!CollectionUtils.isEmpty(eventStatusList), ExecutionNodeEventEntity::getEventStatus, eventStatusList);
        return list(queryWrapper);
    }

    @Override
    public ExecutionNodeEventEntity queryOneNodeEvent(ExecutionNodeEventEntity queryDo) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda()
                .eq(NumberUtils.isPositiveLong(queryDo.getId()), ExecutionNodeEventEntity::getId, queryDo.getId())
                .eq(Objects.nonNull(queryDo.getEventType()), ExecutionNodeEventEntity::getEventType, queryDo.getEventType())
                .eq(!StringUtils.isBlank(queryDo.getEventName()), ExecutionNodeEventEntity::getEventName, queryDo.getEventName())
                .eq(NumberUtils.isPositiveLong(queryDo.getExecutionNodeId()), ExecutionNodeEventEntity::getExecutionNodeId, queryDo.getExecutionNodeId())
                .eq(NumberUtils.isPositiveLong(queryDo.getFlowId()), ExecutionNodeEventEntity::getFlowId, queryDo.getFlowId())
                .eq(NumberUtils.isPositiveInteger(queryDo.getBatchId()), ExecutionNodeEventEntity::getBatchId, queryDo.getBatchId())
                .eq(Objects.nonNull(queryDo.getEventStatus()), ExecutionNodeEventEntity::getEventStatus, queryDo.getEventStatus())
                .eq(Objects.nonNull(queryDo.getReleaseScope()), ExecutionNodeEventEntity::getReleaseScope, queryDo.getReleaseScope())
                .eq(!StringUtils.isBlank(queryDo.getHostName()), ExecutionNodeEventEntity::getHostName, queryDo.getHostName())
                .eq(NumberUtils.isPositiveInteger(queryDo.getExecuteOrder()), ExecutionNodeEventEntity::getExecuteOrder, queryDo.getExecuteOrder())
                .eq(NumberUtils.isPositiveLong(queryDo.getLogId()), ExecutionNodeEventEntity::getLogId, queryDo.getLogId())
                .eq(!StringUtils.isBlank(queryDo.getSchedInstanceId()), ExecutionNodeEventEntity::getSchedInstanceId, queryDo.getSchedInstanceId())
                .eq(Objects.nonNull(queryDo.getStartTime()), ExecutionNodeEventEntity::getStartTime, queryDo.getStartTime())
                .eq(Objects.nonNull(queryDo.getEndTime()), ExecutionNodeEventEntity::getEndTime, queryDo.getEndTime())
                .eq(Objects.nonNull(queryDo.getCtime()), ExecutionNodeEventEntity::getCtime, queryDo.getCtime())
                .eq(Objects.nonNull(queryDo.getMtime()), ExecutionNodeEventEntity::getMtime, queryDo.getMtime())
                .eq(!StringUtils.isBlank(queryDo.getProjectCode()), ExecutionNodeEventEntity::getProjectCode, queryDo.getProjectCode())
                .eq(!StringUtils.isBlank(queryDo.getPipelineCode()), ExecutionNodeEventEntity::getPipelineCode, queryDo.getPipelineCode())
                .eq(!StringUtils.isBlank(queryDo.getTaskCode()), ExecutionNodeEventEntity::getTaskCode, queryDo.getTaskCode())
                .eq(Objects.nonNull(queryDo.getFailureStrategy()), ExecutionNodeEventEntity::getFailureStrategy, queryDo.getFailureStrategy())
                .eq(!StringUtils.isBlank(queryDo.getTaskPosType()), ExecutionNodeEventEntity::getTaskPosType, queryDo.getTaskPosType())
                .eq(NumberUtils.isPositiveLong(queryDo.getJobTaskSetId()), ExecutionNodeEventEntity::getJobTaskSetId, queryDo.getJobTaskSetId())
                .eq(NumberUtils.isPositiveLong(queryDo.getJobTaskId()), ExecutionNodeEventEntity::getJobTaskId, queryDo.getJobTaskId())
                .eq(NumberUtils.isPositiveLong(queryDo.getInstanceId()), ExecutionNodeEventEntity::getInstanceId, queryDo.getInstanceId())
                .eq(ExecutionNodeEventEntity::getDeleted, false)
                .last(Constants.LIMIT_ONE);
        return getOne(queryWrapper);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryNodeEventList(ExecutionNodeEventEntity queryDo) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda()
                .eq(NumberUtils.isPositiveLong(queryDo.getId()), ExecutionNodeEventEntity::getId, queryDo.getId())
                .eq(Objects.nonNull(queryDo.getEventType()), ExecutionNodeEventEntity::getEventType, queryDo.getEventType())
                .eq(!StringUtils.isBlank(queryDo.getEventName()), ExecutionNodeEventEntity::getEventName, queryDo.getEventName())
                .eq(NumberUtils.isPositiveLong(queryDo.getExecutionNodeId()), ExecutionNodeEventEntity::getExecutionNodeId, queryDo.getExecutionNodeId())
                .eq(NumberUtils.isPositiveLong(queryDo.getFlowId()), ExecutionNodeEventEntity::getFlowId, queryDo.getFlowId())
                .eq(NumberUtils.isPositiveInteger(queryDo.getBatchId()), ExecutionNodeEventEntity::getBatchId, queryDo.getBatchId())
                .eq(Objects.nonNull(queryDo.getEventStatus()), ExecutionNodeEventEntity::getEventStatus, queryDo.getEventStatus())
                .eq(Objects.nonNull(queryDo.getReleaseScope()), ExecutionNodeEventEntity::getReleaseScope, queryDo.getReleaseScope())
                .eq(!StringUtils.isBlank(queryDo.getHostName()), ExecutionNodeEventEntity::getHostName, queryDo.getHostName())
                .eq(NumberUtils.isPositiveInteger(queryDo.getExecuteOrder()), ExecutionNodeEventEntity::getExecuteOrder, queryDo.getExecuteOrder())
                .eq(NumberUtils.isPositiveLong(queryDo.getLogId()), ExecutionNodeEventEntity::getLogId, queryDo.getLogId())
                .eq(!StringUtils.isBlank(queryDo.getSchedInstanceId()), ExecutionNodeEventEntity::getSchedInstanceId, queryDo.getSchedInstanceId())
                .eq(Objects.nonNull(queryDo.getStartTime()), ExecutionNodeEventEntity::getStartTime, queryDo.getStartTime())
                .eq(Objects.nonNull(queryDo.getEndTime()), ExecutionNodeEventEntity::getEndTime, queryDo.getEndTime())
                .eq(Objects.nonNull(queryDo.getCtime()), ExecutionNodeEventEntity::getCtime, queryDo.getCtime())
                .eq(Objects.nonNull(queryDo.getMtime()), ExecutionNodeEventEntity::getMtime, queryDo.getMtime())
                .eq(!StringUtils.isBlank(queryDo.getProjectCode()), ExecutionNodeEventEntity::getProjectCode, queryDo.getProjectCode())
                .eq(!StringUtils.isBlank(queryDo.getPipelineCode()), ExecutionNodeEventEntity::getPipelineCode, queryDo.getPipelineCode())
                .eq(!StringUtils.isBlank(queryDo.getTaskCode()), ExecutionNodeEventEntity::getTaskCode, queryDo.getTaskCode())
                .eq(Objects.nonNull(queryDo.getFailureStrategy()), ExecutionNodeEventEntity::getFailureStrategy, queryDo.getFailureStrategy())
                .eq(!StringUtils.isBlank(queryDo.getTaskPosType()), ExecutionNodeEventEntity::getTaskPosType, queryDo.getTaskPosType())
                .eq(NumberUtils.isPositiveLong(queryDo.getJobTaskSetId()), ExecutionNodeEventEntity::getJobTaskSetId, queryDo.getJobTaskSetId())
                .eq(NumberUtils.isPositiveLong(queryDo.getJobTaskId()), ExecutionNodeEventEntity::getJobTaskId, queryDo.getJobTaskId())
                .eq(NumberUtils.isPositiveLong(queryDo.getInstanceId()), ExecutionNodeEventEntity::getInstanceId, queryDo.getInstanceId())
                .eq(ExecutionNodeEventEntity::getDeleted, false);
        return list(queryWrapper);
    }

    @Override
    public void updateBatchNodeEventSchedIdAndPipelineCode(Long flowId, Long instanceId, List<Long> nodeIdList, String projectCode, String pipelineCode, String schedInInstanceId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .in(ExecutionNodeEventEntity::getExecutionNodeId, nodeIdList)
                .set(ExecutionNodeEventEntity::getProjectCode, projectCode)
                .set(ExecutionNodeEventEntity::getPipelineCode, pipelineCode)
                .set(ExecutionNodeEventEntity::getSchedInstanceId, schedInInstanceId);
        update(updateWrapper);
    }

    @Override
    public List<ExecutionNodeEventEntity> queryEventListByFlowIdExecuteOrderInstanceId(Long flowId, Long instanceId, Integer executeOrder) {
        LambdaQueryWrapper<ExecutionNodeEventEntity> queryWrapper = new QueryWrapper<ExecutionNodeEventEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getExecuteOrder, executeOrder);
        return list(queryWrapper);
    }

    @Override
    public void updateBatchEventStatusByIdList(Long flowId, List<Long> eventIdList, EventStatusEnum eventStatus) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .in(ExecutionNodeEventEntity::getId, eventIdList)
                .set(ExecutionNodeEventEntity::getEventStatus, eventStatus);
        update(updateWrapper);
    }

    public boolean updateEventExecDate(Long flowId, Long instanceId, Long eventId, EventStatusEnum eventStatus, LocalDateTime eventEndTime, long jobSetId, long jobTaskId) {
        LambdaUpdateWrapper<ExecutionNodeEventEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEventEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEventEntity::getFlowId, flowId)
                .eq(ExecutionNodeEventEntity::getInstanceId, instanceId)
                .eq(ExecutionNodeEventEntity::getId, eventId)
                .set(Objects.nonNull(eventStatus), ExecutionNodeEventEntity::getEventStatus, eventStatus)
                .set(Objects.nonNull(eventEndTime), ExecutionNodeEventEntity::getEndTime, eventEndTime)
                .set(jobSetId > 0, ExecutionNodeEventEntity::getJobTaskSetId, jobSetId)
                .set(jobTaskId > 0, ExecutionNodeEventEntity::getJobTaskId, jobTaskId);
        return update(updateWrapper);
    }

}
