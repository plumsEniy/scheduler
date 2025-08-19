package com.bilibili.cluster.scheduler.api.event;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public abstract class AbstractTaskEventHandler implements TaskEventHandler {

    @Value("${spring.profiles.active}")
    private String active;

    @Resource
    protected ExecutionFlowService executionFlowService;

    @Resource
    protected ExecutionNodeService executionNodeService;

    @Resource
    protected ExecutionNodeEventService executionNodeEventService;

    @Resource
    protected ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    protected ExecutionNodePropsService executionNodePropsService;

    @Resource
    protected ExecutionLogService executionLogService;

    @Resource
    protected GlobalService globalService;

    @Resource
    protected RedissonLockSupport redissonLockSupport;

    @Resource
    protected BmrFlowService bmrFlowService;

    protected final LogTypeEnum logType = LogTypeEnum.EVENT;

    protected boolean preHandleTaskEvent(TaskEvent taskEvent) throws TaskEventHandleException {
        try {
            log.info("preHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
            Long eventId = taskEvent.getEventId();
            EventStatusEnum eventStatus = taskEvent.getEventStatus();

            ExecutionNodeEntity executionNode = executionNodeService.getById(taskEvent.getExecutionNodeId());
            if (executionNode.getNodeStatus() == NodeExecuteStatusEnum.SKIPPED || EventStatusEnum.SKIPPED == eventStatus) {
                executionLogService.updateLogContent(eventId, logType, "---------------------",
                        String.format("flow job status is SKIPPED, current task event will SKIPPED"));
                taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
                executionNodeEventService.updateEventStatus(taskEvent);
                // executionNodeService.updateNodeStatusById(taskEvent.getExecutionNodeId(), NodeExecuteStatusEnum.SKIPPED);
                return false;
            }
            if (taskEvent.getInstanceId().longValue() != executionNode.getInstanceId().longValue()) {
                log.info("execute node instance id already change: before={}, after={}, skip...", taskEvent.getInstanceId(), executionNode.getInstanceId());
                return false;
            }
            taskEvent.setExecutionNode(executionNode);

            refreshEventFromDB(taskEvent);
            ExecutionNodeEventEntity executionNodeEvent = taskEvent.getEventEntity();
            if (executionNodeEvent.getEventStatus() == EventStatusEnum.SUCCEED_EVENT_EXECUTE) {
                executionLogService.updateLogContent(eventId, logType, "---------------------",
                        String.format("job event is success, will skip"));
                taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
                executionNodeEventService.updateEventStatus(taskEvent);
                return false;
            }

            if (executionNodeEvent.getEventStatus() == EventStatusEnum.EVENT_SKIPPED) {
                executionLogService.updateLogContent(eventId, logType, "---------------------",
                        String.format("job event is EVENT_SKIPPED state, will skip"));
                taskEvent.setEventStatus(EventStatusEnum.EVENT_SKIPPED);
                executionNodeEventService.updateEventStatus(taskEvent);
                return false;
            }

            boolean eventIsRequired = checkEventIsRequired(taskEvent);
            if (!eventIsRequired) {
                logPersist(taskEvent, "current event is not required, pre-handler skip it");
                return false;
            }

            // 更改事件执行状态为执行中
            taskEvent.setEventStatus(EventStatusEnum.IN_EVENT_EXECUTE);
            executionNodeEventService.updateEventStatus(taskEvent);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TaskEventHandleException(e.getMessage(), e);
        }
    }

    /**
     * judge with not required event, return false
     *
     * @param taskEvent
     * @return
     */
    protected boolean checkEventIsRequired(TaskEvent taskEvent) {

        if (isLogicNode(taskEvent) && skipLogicNode()) {
            return false;
        }

        if (isInRollbackStatus(taskEvent) && skipRollbackStatus()) {
            return false;
        }

        return true;
    }

    /**
     * 逻辑节点是否跳过
     * @return
     */
    protected boolean skipLogicNode() {
        return false;
    }

    /**
     * 回滚状态是否跳过
     * @return
     */
    protected boolean skipRollbackStatus() {
        return false;
    }

    protected void refreshEventFromDB(TaskEvent taskEvent) {
        ExecutionNodeEventEntity eventEntity = executionNodeEventService.getById(taskEvent.getEventId());
        Preconditions.checkState(eventEntity.getInstanceId().longValue() == taskEvent.getInstanceId().longValue(),
                "instanceId already change, memory is {}, db is {}, require give up current event handle...",
                taskEvent.getInstanceId(), eventEntity.getInstanceId());
        taskEvent.setEventEntity(eventEntity);
    }

    protected String getLockKey(TaskEvent taskEvent, RedisLockKey lockKey) {
        StringBuilder builder = new StringBuilder();
        builder.append(lockKey.name())
                .append(Constants.UNDER_LINE)
                .append(taskEvent.getFlowId())
                .append(Constants.UNDER_LINE)
                .append(taskEvent.getInstanceId());
        return builder.toString();
    }

    protected boolean isFirstEvent(TaskEvent event) {
        return event.getExecuteOrder() == 1;
    }

    /**
     * @param retryTimes
     * @param func
     * @param param
     * @param <T>
     * @param <R>
     * @return
     * @throws Exception
     */
    protected static <T, R> R funcRetry(int retryTimes, Function<T, R> func, T param) throws Exception {
        int i = 0;
        Exception finalException;
        R result = null;
        do {
            try {
                finalException = null;
                result = func.apply(param);
                break;
            } catch (Exception e) {
                finalException = e;
                Thread.sleep((i + 1) * 500);
            }
        } while (i++ < retryTimes);
        if (finalException != null) {
            throw finalException;
        }
        return result;
    }

    protected void logPersist(TaskEvent taskEvent, String logContent) {
        executionLogService.updateLogContent(taskEvent.getEventId(), logType, logContent);
    }

    /**
     * dolphin-scheduler类型任务需要剔除job-agent未安装节点
     *
     * @param hostList
     * @param taskEvent
     */
    protected void filterOutLostJobAgentHosts(List<String> hostList, TaskEvent taskEvent) {
        List<String> lostJobAgentList = globalService.getJobAgentService().queryLostJobAgent(hostList);
        if (CollectionUtils.isEmpty(lostJobAgentList)) {
            return;
        }

        List<Long> lostJobAgentNodeIdList = new ArrayList<>();
        for (String hostname : lostJobAgentList) {
            ExecutionNodeEntity nodeEntity = executionNodeService.queryByHostnameAndInstanceId(
                    taskEvent.getFlowId(), hostname, taskEvent.getInstanceId());
            if (Objects.isNull(nodeEntity)) {
                continue;
            }
            lostJobAgentNodeIdList.add(nodeEntity.getId());
            executionNodeService.updateNodeOperationResultByNodeId(nodeEntity.getId(), NodeOperationResult.JOB_AGENT_LOST, taskEvent.getInstanceId());
            executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(), nodeEntity.getId(),
                    taskEvent.getEventEntity().getTaskCode(), null,
                    EventStatusEnum.FAIL_EVENT_EXECUTE, LocalDateTime.now(), 0, 0);
        }
        if (CollectionUtils.isEmpty(lostJobAgentNodeIdList)) {
            return;
        }

        String msg = String.format("prepare submit to dolphin-scheduler, but nodes of %s log-agent status was 'LOST' or 'NOT_INSTALL', skip them...",
                JSONUtil.toJsonStr(lostJobAgentList));
        logPersist(taskEvent, msg);
    }

    /**
     * 全局任务的globalhandler
     *
     * @param taskEvent
     * @return
     * @throws TaskEventHandleException
     */
    protected boolean globalPreHandler(TaskEvent taskEvent) throws TaskEventHandleException {
        try {
            log.info("preHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
            Long eventId = taskEvent.getEventId();

            ExecutionNodeEventEntity executionNodeEvent = executionNodeEventService.getById(eventId);
            ExecutionNodeEntity executionNode = executionNodeService.getById(taskEvent.getExecutionNodeId());
            EventStatusEnum eventStatus = executionNodeEvent.getEventStatus();

//            成功则跳过
            if (executionNodeEvent.getEventStatus().equals(EventStatusEnum.SUCCEED_EVENT_EXECUTE)) {
                log.info("job event is success ,will skip");
                taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
                executionNodeEventService.updateEventStatus(taskEvent);
                return false;
            }

//        未执行则开始执行
            if (eventStatus.equals(EventStatusEnum.UN_EVENT_EXECUTE)) {
                log.info("flow job status is UN_EVENT_EXECUTE, current task event will execute");
                taskEvent.setEventStatus(EventStatusEnum.IN_EVENT_EXECUTE);
                taskEvent.setStartTime(LocalDateTime.now());
                taskEvent.setExecutionNode(executionNode);
                executionNodeEventService.updateGlobalEventStatus(taskEvent);
            }

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TaskEventHandleException(e.getMessage(), e);
        }
    }

    protected boolean batchPreHandler(TaskEvent taskEvent) throws TaskEventHandleException {
        try {
            log.info("preHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
            Long eventId = taskEvent.getEventId();

            ExecutionNodeEventEntity executionNodeEvent = executionNodeEventService.getById(eventId);
            ExecutionNodeEntity executionNode = executionNodeService.getById(taskEvent.getExecutionNodeId());
            EventStatusEnum eventStatus = executionNodeEvent.getEventStatus();

            Long flowId = taskEvent.getFlowId();
            Integer executeOrder = taskEvent.getExecuteOrder();
            Integer batchId = taskEvent.getBatchId();

            //            成功则跳过
            if (executionNodeEvent.getEventStatus().equals(EventStatusEnum.SUCCEED_EVENT_EXECUTE)) {
                executionLogService.updateLogContent(eventId, LogTypeEnum.EVENT, "---------------------", String.format("job event is success ,will skip"));
                taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
                executionNodeEventService.updateEventStatus(taskEvent);
                return false;
            }


//        未执行则开始执行
            if (eventStatus.equals(EventStatusEnum.UN_EVENT_EXECUTE)) {
                executionLogService.updateBatchEventLog(flowId, executeOrder, batchId, "---------------------", String.format("flow job status is UN_EVENT_EXECUTE, current task event will execute"));
                taskEvent.setEventStatus(EventStatusEnum.IN_EVENT_EXECUTE);
                taskEvent.setStartTime(LocalDateTime.now());
                taskEvent.setExecutionNode(executionNode);
                executionNodeEventService.updateBatchEventStatus(taskEvent);
            }

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TaskEventHandleException(e.getMessage(), e);
        }
    }


    protected void afterHandleTaskEvent(boolean result, TaskEvent taskEvent) throws TaskEventHandleException {
        try {
            // log.info("afterHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
            EventStatusEnum eventStatus = taskEvent.getEventStatus();
            Long executionJobId = taskEvent.getExecutionNodeId();
            NodeOperationResult operationResult = taskEvent.getOperationResult();
            taskEvent.setEndTime(LocalDateTime.now());
            if (EventStatusEnum.SKIPPED.equals(eventStatus)) {
                taskEvent.setEventStatus(EventStatusEnum.SKIPPED);
                executionNodeEventService.updateEventStatus(taskEvent);
                executionNodeService.updateNodeStatusById(executionJobId, NodeExecuteStatusEnum.SKIPPED);
                if (!Objects.isNull(operationResult)) {
                    executionNodeService.updateNodeOperationResultByNodeId(executionJobId, operationResult, taskEvent.getInstanceId());
                }
                return;
            }
            if (result) {
                afterTaskEventFinish(taskEvent);
                taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
                executionNodeService.updateNodeOperationResultByNodeId(executionJobId, NodeOperationResult.NORMAL, taskEvent.getInstanceId());
                executionNodeEventService.updateEventStatus(taskEvent);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TaskEventHandleException(e.getMessage(), e);
        }
    }

    protected void afterTaskEventFinish(TaskEvent taskEvent) {

    }

    protected void globalAfterHandler(boolean result, TaskEvent taskEvent) {
        log.info("afterHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
        taskEvent.setEndTime(LocalDateTime.now());
        if (result) {
            taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
        } else {
            taskEvent.setEventStatus(EventStatusEnum.FAIL_EVENT_EXECUTE);
        }
        executionNodeEventService.updateGlobalEventStatus(taskEvent);
    }

    protected void batchAfterHandler(boolean result, TaskEvent taskEvent) {
        log.info("afterHandleTaskEvent:{}", JSONUtil.toJsonStr(taskEvent));
        taskEvent.setEndTime(LocalDateTime.now());
        if (result) {
            taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
        } else {
            taskEvent.setEventStatus(EventStatusEnum.FAIL_EVENT_EXECUTE);
        }
        executionNodeEventService.updateBatchEventStatus(taskEvent);
    }


    public boolean handleTaskEvent(TaskEvent taskEvent) throws TaskEventHandleException {
        try {
            boolean preHandleTaskEvent = preHandleTaskEvent(taskEvent);
            if (preHandleTaskEvent) {
                boolean result = executeTaskEvent(taskEvent);
                afterHandleTaskEvent(result, taskEvent);
                return result;
            } else {
                afterTaskEventFinish(taskEvent);
                if (Objects.isNull(taskEvent.getEventStatus())) {
                    taskEvent.setEventStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE);
                }
                taskEvent.setEndTime(LocalDateTime.now());
                executionNodeEventService.updateEventStatus(taskEvent);
            }
            return true;
        } catch (Exception e) {
            EventTypeEnum eventTypeEnum = taskEvent.getEventTypeEnum();
            String errorMsg = String.format("%s error, error message is %s", eventTypeEnum.getDesc(), e.getMessage());
            logPersist(taskEvent, errorMsg);
            log.error(e.getMessage(), e);
            throw new TaskEventHandleException(e.getMessage(), e);
        }
    }

    public abstract boolean executeTaskEvent(TaskEvent taskEvent) throws Exception;

    protected boolean isInRollbackStatus(TaskEvent taskEvent) {
        Long executionNodeId = taskEvent.getExecutionNodeId();
        ExecutionNodeEntity executionNode = executionNodeService.getById(executionNodeId);
        NodeExecuteStatusEnum nodeStatus = executionNode.getNodeStatus();
        return nodeStatus.isInRollback();
    }

    protected boolean isLogicNode(TaskEvent taskEvent) {
        Long executionNodeId = taskEvent.getExecutionNodeId();
        ExecutionNodeEntity executionNode = executionNodeService.getById(executionNodeId);
        NodeType nodeType = executionNode.getNodeType();
        return !nodeType.isNormalNode();
    }

}
