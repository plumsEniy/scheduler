package com.bilibili.cluster.scheduler.api.scheduler.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.TaskEventHandler;
import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import com.bilibili.cluster.scheduler.api.service.cache.CacheService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.status.ExecuteFlowStatusProcess;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowProps;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkflowStartInstanceTaskEventHandler extends AbstractWorkflowInstanceTaskEventHandler implements WorkflowInstanceTaskEventHandler {

    private ThreadPoolExecutor workflowStartInstanceTaskEventExecService;

    @Resource
    private List<TaskEventHandler> taskEventHandlerList;

    private final Map<EventTypeEnum, TaskEventHandler> taskEventHandlerMap = new HashMap<>();


    @Resource
    private ExecutionNodeService executionNodeService;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    private ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    private CacheService cacheService;

    @Resource
    private ExecuteFlowStatusProcess executeFlowStatusProcess;

    private Retryer<Boolean> retry = RetryerBuilder.<Boolean>newBuilder()
            .retryIfException()
            // 运行时异常时
            .retryIfRuntimeException()
            // call方法返回true时重试
            .retryIfResult(ab -> Objects.equals(ab, true))
            // 10秒后重试
            .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
            // 重试n次，超过次数就...
            .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 重试3次后停止
            .build();

    @PostConstruct
    public void init() {
        this.workflowStartInstanceTaskEventExecService = (ThreadPoolExecutor) ThreadUtils
                .newDaemonFixedThreadExecutor(" WorkflowStartInstanceTaskEventExecThread", 1000);
        taskEventHandlerList.forEach(taskEventHandler -> taskEventHandlerMap.put(taskEventHandler.getHandleEventType(), taskEventHandler));
    }

    @Override
    public void executeWorkflowInstanceTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent) throws WorkflowInstanceTaskEventHandleException {
        try {

            ExecutionFlowInstanceDTO executionFlowInstanceDTO = workflowInstanceTaskEvent.getExecutionFlowInstanceDTO();
            Long flowId = executionFlowInstanceDTO.getFlowId();
            ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(flowId);
            ExecutionFlowProps executionFlowProps = new ExecutionFlowProps();
            BeanUtils.copyProperties(executionFlowEntity, executionFlowProps);

            // 查询待执行的job, 通过instanceId, 更细粒度的控制发布节点, 而非batchId维度
            Long instanceId = executionFlowInstanceDTO.getInstanceId();
            final List<ExecutionNodeEntity> executionNodeEntities = executionNodeService.queryNodeListByInstanceId(flowId, instanceId);

            handleWorkflowJobTaskEvent(workflowInstanceTaskEvent, executionNodeEntities);
        } catch (Exception e) {
            log.error("handleWorkflowInstanceTaskEvent has error", e);
            throw new WorkflowInstanceTaskEventHandleException(e.getMessage(), e);
        }
    }

    @Override
    public void handleWorkflowJobTaskEvent(WorkflowInstanceTaskEvent workflowInstanceTaskEvent, List<ExecutionNodeEntity> executionNodeEntities) throws WorkflowInstanceTaskEventHandleException {
        try {
            log.info("executionJobEntities size is :{}", executionNodeEntities.size());
            if (CollectionUtils.isEmpty(executionNodeEntities)) {
                log.error("executionJobEntities is null....");
                return;
            }

            ExecutionFlowInstanceDTO executionFlowInstanceDTO = workflowInstanceTaskEvent.getExecutionFlowInstanceDTO();
            Long flowId = executionFlowInstanceDTO.getFlowId();
            Long instanceId = executionFlowInstanceDTO.getInstanceId();
            List<Long> nodeIdList = executionNodeEntities.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());

            // update current instanceId to node and event, update event status require by it
            log.info("flowId {} update current instanceId to node and event, instanceId is {}", flowId, instanceId);
            executionNodeService.updateNodeAndEventInstanceId(flowId, instanceId, nodeIdList, workflowInstanceTaskEvent.getHostName());

            for (ExecutionNodeEntity executionNodeEntity : executionNodeEntities) {
                execPeerNodePipelineEvent(executionNodeEntity, executionFlowInstanceDTO);
            }

        } catch (Exception e) {
            log.error("handleWorkflowJobTaskEvent has error", e);
            throw new WorkflowInstanceTaskEventHandleException(e.getMessage(), e);
        }
    }

    private void execPeerNodePipelineEvent(ExecutionNodeEntity executionNodeEntity, ExecutionFlowInstanceDTO executionFlowInstanceDTO) {
        Long executionNodeId = executionNodeEntity.getId();
        long instanceId = executionFlowInstanceDTO.getInstanceId();
        workflowStartInstanceTaskEventExecService.execute(() -> {
            try {
                MDC.put(Constants.LOG_TRACE_ID, IdUtil.simpleUUID());
                // refresh from db
                ExecutionNodeEntity nodeEntity = executionNodeService.getById(executionNodeId);
                log.info("execPeerNodePipelineEvent, nodeId:{}, instanceId:{},node info:{}", executionNodeId, instanceId, JSONUtil.toJsonStr(nodeEntity));
                if (nodeEntity.getInstanceId().longValue() != instanceId) {
                    log.warn("current node {} instance id already change: before={}, after={}, skip...",
                            nodeEntity.getNodeName(), instanceId, nodeEntity.getInstanceId());
                    return;
                }

                NodeExecuteStatusEnum beforeStatus = nodeEntity.getNodeStatus();
                // 完成待执行到执行中的状态转换
                NodeExecuteStatusEnum nextStatus = NodeExecuteStatusEnum.getNextExecStatus(beforeStatus);
                if (beforeStatus != nextStatus) {
                    executionNodeService.updateNodeStatusByIdAndInstanceId(executionNodeId, instanceId, nextStatus);
                    log.info("switch node {} execute status from {} transform to {}.",
                            nodeEntity.getNodeName(), beforeStatus, nextStatus);
                }
                String nodeName = nodeEntity.getNodeName();

                // 更新 bmr_execution_job 表任务start_time
                if (nodeEntity.getStartTime().getYear() == 1970) {
                    executionNodeService.updateNodeStartTimeOrEndTime(executionNodeId, instanceId, LocalDateTime.now(), null);
                }
                List<ExecutionNodeEventEntity> nodeEventEntityList = executionNodeEventService.queryByExecutionNodeIdAndFlowId(
                        executionFlowInstanceDTO.getFlowId(), executionNodeId);
                if (CollectionUtils.isEmpty(nodeEventEntityList)) {
                    log.error("jobEventEntityList is null, executionJobId:{}, nodeName:{}", executionNodeId, nodeName);
                    return;
                }

                Collections.sort(nodeEventEntityList);
                Iterator<ExecutionNodeEventEntity> iterator = nodeEventEntityList.iterator();

                while (iterator.hasNext()) {
                    ExecutionNodeEventEntity executionNodeEventEntity = iterator.next();

                    TaskEvent taskEvent = transferToTaskEvent(executionNodeEventEntity, nodeEntity, executionFlowInstanceDTO);
                    TaskEventHandler taskEventHandler = taskEventHandlerMap.get(taskEvent.getEventTypeEnum());
                    int maxAttemptNumber = taskEvent.getMaxAttemptNumber();
                    Retryer<Boolean> retry = RetryerBuilder.<Boolean>newBuilder()
                            .retryIfException()
                            // 运行时异常时
                            .retryIfExceptionOfType(TaskEventHandleException.class)
                            .retryIfRuntimeException() // callable抛出RuntimeException重试
                            // call方法返回false时重试
                            .retryIfResult(result -> Objects.equals(result, false))
                            // 10秒后重试
                            .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                            .withRetryListener(new RetryListener() {

                                @Override
                                public <Boolean> void onRetry(Attempt<Boolean> attempt) {
                                    long attemptNumber = attempt.getAttemptNumber();
                                    NodeOperationResult operationResult = taskEvent.getOperationResult();
                                    if (Objects.isNull(operationResult)) {
                                        operationResult = NodeOperationResult.UNKNOWN;
                                    }

                                    log.info("retry event execute, attemptNumber:{}, taskEvent:{}, node:{}", attemptNumber, taskEvent.getEventId(), taskEvent.getNodeName());
                                    if (attempt.hasException()) {
                                        Throwable exceptionCause = attempt.getExceptionCause();
                                        String message = exceptionCause.getMessage();
                                        String format = String.format("eventType: %s retry exception:%s, attemptNumber:%s", taskEvent.getEventTypeEnum(), message, attemptNumber);
                                        log.error(format, exceptionCause);

                                        if (attemptNumber >= maxAttemptNumber) {
                                            taskEvent.setEndTime(LocalDateTime.now());
                                            taskEvent.setEventStatus(EventStatusEnum.FAIL_EVENT_EXECUTE);
                                            executionNodeEventService.updateEventStatus(taskEvent);
                                            executionNodeService.updateNodeOperationResultByNodeId(
                                                    taskEvent.getExecutionNodeId(), operationResult, instanceId);
                                        }
                                        return;
                                    }

                                    Boolean result = attempt.getResult();
                                    if (attemptNumber >= maxAttemptNumber && java.lang.Boolean.FALSE.equals(result)) {
                                        taskEvent.setEndTime(LocalDateTime.now());
                                        taskEvent.setEventStatus(EventStatusEnum.FAIL_EVENT_EXECUTE);
                                        executionNodeEventService.updateEventStatus(taskEvent);
                                        executionNodeService.updateNodeOperationResultByNodeId(
                                                taskEvent.getExecutionNodeId(), operationResult, instanceId);
                                    }
                                }
                            })
                            // 重试n次，超过次数就...
                            .withStopStrategy(StopStrategies.stopAfterAttempt(maxAttemptNumber))
                            .build();
                    retry.call(() -> taskEventHandler.handleTaskEvent(taskEvent));
                }
                // may we need update node status as a fast path when all event finish
                executeFlowStatusProcess.updateNodeExecuteStatus(executionNodeId);
            } catch (Exception e) {
                log.error("handleTaskEvent has error", e);
                executeFlowStatusProcess.updateNodeExecuteStatus(executionNodeId);
            }
            executionNodeService.updateNodeStartTimeOrEndTime(executionNodeEntity.getId(), instanceId, null, LocalDateTime.now());
            MDC.remove(Constants.LOG_TRACE_ID);
        });
    }

    private TaskEvent transferToTaskEvent(ExecutionNodeEventEntity executionNodeEventEntity,
                                          ExecutionNodeEntity executionNodeEntity,
                                          ExecutionFlowInstanceDTO executionFlowInstanceDTO) {
        EventTypeEnum eventType = executionNodeEventEntity.getEventType();
        TaskEvent taskEvent = new TaskEvent();
        taskEvent.setStartTime(LocalDateTime.now());
        taskEvent.setExecuteOrder(executionNodeEventEntity.getExecuteOrder());
        taskEvent.setInstanceId(executionFlowInstanceDTO.getInstanceId());
        taskEvent.setFlowId(executionFlowInstanceDTO.getFlowId());
        taskEvent.setEventId(executionNodeEventEntity.getId());
        taskEvent.setBatchId(executionNodeEntity.getBatchId());
        taskEvent.setExecutionNodeId(executionNodeEntity.getId());
        taskEvent.setDeployType(executionFlowInstanceDTO.getDeployType());
        taskEvent.setNodeName(executionNodeEntity.getNodeName());
        taskEvent.setEventTypeEnum(eventType);
        taskEvent.setHostName(executionFlowInstanceDTO.getHostName());
        taskEvent.setExecutionFlowInstanceDTO(executionFlowInstanceDTO);
        taskEvent.setReleaseScope(executionNodeEventEntity.getReleaseScope());

        // 是否开启了进行重试
        boolean autoRetry = executionFlowInstanceDTO.isAutoRetry() && eventType.isSupportRetry();
        final int maxAttemptNumber;
        if (autoRetry) {
            maxAttemptNumber = executionFlowInstanceDTO.getMaxRetry() + 1;
        } else {
            maxAttemptNumber = 1;
        }
        taskEvent.setAutoRetry(autoRetry);
        taskEvent.setMaxAttemptNumber(maxAttemptNumber);
        return taskEvent;
    }
}
