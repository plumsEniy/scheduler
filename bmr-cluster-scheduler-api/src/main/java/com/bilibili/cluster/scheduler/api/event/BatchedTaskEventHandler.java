package com.bilibili.cluster.scheduler.api.event;

import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class BatchedTaskEventHandler extends AbstractTaskEventHandler {

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        refreshEventFromDB(taskEvent);
        int waitLoopTime = 0;

        while (true) {
            if (waitLoopTime % 10 == 0) {
                tryAlignAndExecTaskEvent(taskEvent);
            }

            refreshEventFromDB(taskEvent);
            EventStatusEnum eventStatus = taskEvent.getEventEntity().getEventStatus();
            switch (eventStatus) {
                case SKIPPED:
                case SUCCEED_EVENT_EXECUTE:
                    return true;
                case FAIL_EVENT_EXECUTE:
                    return false;
            }
            waitLoopTime++;
            long randomSleepTs = getMinLoopWait() + taskEvent.getRandom().nextInt(getMaxLoopStep());
            ThreadUtils.sleep(randomSleepTs);
        }
    }

    protected boolean tryAlignAndExecTaskEvent(TaskEvent taskEvent) throws Exception {
        Boolean isPreAlignLock = false;
        String preAlignLockKey = getLockKey(taskEvent, RedisLockKey.BMR_DEPLOY_BATCH_TASK_EVENT_PRE_ALIGN_LOCK_KEY);
        try {
            isPreAlignLock = redissonLockSupport.tryLock(preAlignLockKey, getMinLoopWait(), -1, TimeUnit.MILLISECONDS);
            if (isPreAlignLock) {
                refreshEventFromDB(taskEvent);
                EventStatusEnum eventStatus = taskEvent.getEventEntity().getEventStatus();
                if (eventStatus.isFinish()) {
                    return true;
                }
            } else {
                return false;
            }
            String message = String.format("current task %s hold the preAlignLockKey，will wait pre-events align...", taskEvent.getSummary());
            log(taskEvent, message);
            boolean alignment = waitAlignPreTaskEvent(taskEvent);
            Preconditions.checkState(alignment, "BatchedTaskEventHandler wait alignment peer nodes event error");

            message = String.format("current task %s already aligned, will start batch event execute", taskEvent.getSummary());
            log(taskEvent, message);

            List<ExecutionNodeEntity> nodeEntityList = executionNodeService.getAlignNodeListByEventStatus(
                    taskEvent.getFlowId(), taskEvent.getInstanceId(),
                    taskEvent.getExecuteOrder() - 1,
                    Arrays.asList(EventStatusEnum.SUCCEED_EVENT_EXECUTE, EventStatusEnum.SKIPPED));

            if (CollectionUtils.isEmpty(nodeEntityList)) {
                String msg = String.format("not find any node require execute dolphin pipeline events, task detail %s. skip...", taskEvent.getSummary());
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            // 更新logId
            Long logId = executionLogService.queryLogIdByExecuteId(taskEvent.getEventId(), LogTypeEnum.EVENT);
            if (logId > 0) {
                executionNodeEventService.updateBatchEventLogId(taskEvent.getFlowId(), taskEvent.getInstanceId(), taskEvent.getExecuteOrder(),
                        nodeEntityList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList()), logId);
            }

            boolean isSucc = batchExecEvent(taskEvent, nodeEntityList);
            log(taskEvent, "handler batchExecEvent result is: " + isSucc);
            if (isSucc) {
                updateBatchEventStatus(taskEvent, nodeEntityList, EventStatusEnum.SUCCEED_EVENT_EXECUTE);
            } else {
                updateBatchEventStatus(taskEvent, nodeEntityList, EventStatusEnum.FAIL_EVENT_EXECUTE);
            }
            return isSucc;
        } finally {
            if (isPreAlignLock) {
                redissonLockSupport.unLock(preAlignLockKey);
            }
        }
    }

    protected void updateBatchEventStatus(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList, EventStatusEnum eventStatus) {
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return;
        }
        List<Long> nodeIdList = nodeEntityList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
        taskEvent.setEventStatus(eventStatus);
        taskEvent.setEndTime(LocalDateTime.now());
        executionNodeEventService.updateBatchEventStatus(taskEvent, nodeIdList);
    }

    protected boolean getCurrentAlignmentState(TaskEvent taskEvent) throws Exception {
        if (isFirstEvent(taskEvent)) {
            return true;
        }

        long instanceId = taskEvent.getInstanceId();
        long flowId = taskEvent.getFlowId();
        List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeListByInstanceId(flowId, instanceId);
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            log.error("getCurrentAlignmentState but nodeEntityList is blank, flowId {}, instanceId {}", flowId, instanceId);
            throw new TaskEventHandleException("getCurrentAlignmentState but nodeEntityList is blank");
        }
        int total = nodeEntityList.size();
        int alreadyAlignNodeCount = 0;
        for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
            NodeExecuteStatusEnum nodeStatus = nodeEntity.getNodeStatus();
            if (nodeStatus.isFinish()) {
                alreadyAlignNodeCount++;
                continue;
            }
            int preOrder = taskEvent.getExecuteOrder() - 1;

            List<ExecutionNodeEventEntity> preEventList = executionNodeEventService.queryPrePipelineEvents(flowId, instanceId, nodeEntity.getId(), preOrder);
            if (CollectionUtils.isEmpty(preEventList)) {
                alreadyAlignNodeCount++;
                continue;
            }
            Collections.reverse(preEventList);
            ExecutionNodeEventEntity preEventEntity = preEventList.get(0);
            EventStatusEnum eventStatus = preEventEntity.getEventStatus();

            if (eventStatus.isFinish()) {
                alreadyAlignNodeCount++;
                continue;
            }
            log.info("wait dolphin align, but node of {} pre-event {} is not finish.",
                    nodeEntity.getNodeName(), preEventEntity.getEventName());
            return false;
        }

        if (alreadyAlignNodeCount >= total) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean waitAlignPreTaskEvent(TaskEvent taskEvent) throws Exception {
        if (taskEvent.getExecuteOrder().intValue() == 1) {
            log(taskEvent, "this task is first execute event, already aligned.");
            return true;
        }
        int loop = 0;
        while (!getCurrentAlignmentState(taskEvent)) {
            if (loop % logMod() == 0) {
                String message = String.format("task of %s wait align pre-task event, will check next loop...", taskEvent.getSummary());
                log(taskEvent, message);
            }
            long randomSleepTs = getMinLoopWait() + taskEvent.getRandom().nextInt(getMaxLoopStep());
            ThreadUtils.sleep(randomSleepTs);
            loop++;
        }
        return true;
    }

    public abstract boolean batchExecEvent(TaskEvent taskEvent, List<ExecutionNodeEntity> nodeEntityList) throws Exception;

    public abstract int getMinLoopWait();

    public abstract int getMaxLoopStep();

    public void log(TaskEvent taskEvent, String logContent) {
        logPersist(taskEvent, logContent);
        printLog(taskEvent, logContent);
    }

    public abstract void printLog(TaskEvent taskEvent, String logContent);

    public abstract int logMod();

}
