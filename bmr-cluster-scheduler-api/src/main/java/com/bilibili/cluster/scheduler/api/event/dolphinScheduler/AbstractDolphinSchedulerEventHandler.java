package com.bilibili.cluster.scheduler.api.event.dolphinScheduler;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.event.AbstractTaskEventHandler;
import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.env.DolphinEnvExtendedService;
import com.bilibili.cluster.scheduler.api.exceptions.TaskEventHandleException;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupDo;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.HostAndLogicGroupInfo;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomDetail;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskSetData;
import com.bilibili.cluster.scheduler.common.dto.scheduler.ExecutionInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.TaskInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.JobAgentResultDO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinWorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.enums.dolphin.TaskPosType;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.jobAgent.JobAgentTaskState;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.DateTimeUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractDolphinSchedulerEventHandler extends AbstractTaskEventHandler {

    private final int MIN_LOOP_WAIT_TIME = 3_000;

    private final int MAX_LOOP_WAIT_TIME = 10_000;

    private final int LOG_LOOP_MOD = 10;

    @Value("${dolphin.deploy.token}")
    private String deployToken;

    @Resource
    protected DolphinEnvExtendedService dolphinEnvExtendedService;

    @Override
    public boolean executeTaskEvent(TaskEvent taskEvent) throws Exception {
        try {
            MDC.put(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY, getInvokerToken());
            // 查询持久化event基础信息
            refreshEventFromDB(taskEvent);
            String schedInstanceId = taskEvent.getEventEntity().getSchedInstanceId();
            boolean isAlreadySubmit = !StringUtils.isBlank(schedInstanceId);

            if (!isAlreadySubmit) {
                schedInstanceId = getSubmitDolphinSchedInstanceId(taskEvent);
                Preconditions.checkNotNull(schedInstanceId, "schedInstanceId is null.");
            }
            return updateCurrentEventStatus(taskEvent, schedInstanceId);
        } finally {
            MDC.remove(Constants.DOLPHIN_SCHEDULER_TOKEN_KEY);
        }
    }

    private String getSubmitDolphinSchedInstanceId(TaskEvent taskEvent) throws Exception {
        boolean isStartNode = judgeIsStartDolphinPipelineEvent(taskEvent.getEventEntity());
        Preconditions.checkState(isStartNode,
                "trySubmitDolphinPipeline but not dolphin pipeline start node: %s",
                taskEvent.getEventName());
        int waitLoopTime = 0;

        while (true) {
            if (waitLoopTime % logMod() == 0) {
                trySubmitDolphinPipeline(taskEvent);
            }

            refreshEventFromDB(taskEvent);
            String schedInstanceId = taskEvent.getEventEntity().getSchedInstanceId();
            if (!StringUtils.isBlank(schedInstanceId)) {
                return schedInstanceId;
            }

            waitLoopTime++;
            long randomSleepTs = getMinLoopWait() + taskEvent.getRandom().nextInt(getMaxLoopStep());
            ThreadUtils.sleep(randomSleepTs);
        }
    }

    private boolean judgeIsStartDolphinPipelineEvent(ExecutionNodeEventEntity nodeEventEntity) {
        if (nodeEventEntity.getExecuteOrder() == 1) {
            return true;
        }
        TaskPosType taskPosType = TaskPosType.valueOf(nodeEventEntity.getTaskPosType());
        return taskPosType.isStartNode();
    }

    protected boolean trySubmitDolphinPipeline(TaskEvent taskEvent) throws Exception {
        Boolean isPreAlignLock = false;
        String preAlignLockKey = getLockKey(taskEvent, RedisLockKey.BMR_DEPLOY_DOLPHIN_SCHEDULER_PRE_ALIGN_LOCK_KEY);
        try {
            isPreAlignLock = redissonLockSupport.tryLock(preAlignLockKey, getMinLoopWait(), -1, TimeUnit.MILLISECONDS);
            if (isPreAlignLock) {
                long randomSleepTs = getMinLoopWait() + taskEvent.getRandom().nextInt(getMaxLoopStep());
                ThreadUtils.sleep(randomSleepTs);

                // double check already submit or not
                refreshEventFromDB(taskEvent);
                String schedInstanceId = taskEvent.getEventEntity().getSchedInstanceId();
                if (!StringUtils.isBlank(schedInstanceId)) {
                    return true;
                }
                String message = taskEvent.getNodeName() + ": current task hold the redis [PreAlignLockKey]，will wait pre-events align...";
                log.info(message);
                logPersist(taskEvent, message);
                boolean alignment = waitAlignmentPeerEvents(taskEvent);
                Preconditions.checkState(alignment, "DolphinSchedulerEventHandler wait alignment peer nodes event error");

                message = taskEvent.getNodeName() + ": current task already aligned, will start dolphin pipeline execute ...";
                log.info(message);
                logPersist(taskEvent, message);

                // List<ExecutionNodeEntity> nodeEntityList = getCurrentBatchNodeList(taskEvent.getFlowId(), taskEvent.getInstanceId());
                List<ExecutionNodeEntity> nodeEntityList = executionNodeService.getAlignNodeListByEventStatus(
                        taskEvent.getFlowId(), taskEvent.getInstanceId(),
                        taskEvent.getExecuteOrder() - 1,
                        Arrays.asList(EventStatusEnum.SUCCEED_EVENT_EXECUTE, EventStatusEnum.SKIPPED));

                if (CollectionUtils.isEmpty(nodeEntityList)) {
                    String msg = String.format("not find any node require execute dolphin pipeline events, task detail %s. skip...", taskEvent.getSummary());
                    log.error(msg);
                    throw new IllegalArgumentException(msg);
                }

                List<String> hostList = new ArrayList<>();
                // 过滤出执行中的节点
                List<Long> nodeIdList = nodeEntityList.stream().filter(node -> {
                    NodeExecuteStatusEnum nodeStatus = node.getNodeStatus();
                    boolean inExec = nodeStatus.isInExecute();
                    if (inExec) {
                        hostList.add(node.getNodeName());
                    }
                    return inExec;
                }).map(ExecutionNodeEntity::getId).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(nodeIdList)) {
                    log.error("filter node status require in execute, but find none...., flowId {}, instanceId {}",
                            taskEvent.getFlowId(), taskEvent.getInstanceId());
                    return false;
                }
                // 更新event执行日志信息
                Long logId = executionLogService.queryLogIdByExecuteId(taskEvent.getEventId(), LogTypeEnum.EVENT);
                executionNodeEventService.updateBatchEventLogId(taskEvent.getFlowId(), taskEvent.getInstanceId(),
                        taskEvent.getExecuteOrder(), nodeIdList, logId);

                // check job-agent liveness, filter out lost job-agent node to failed status
                filterOutLostJobAgentHosts(hostList, taskEvent);

                // generate dolphin-scheduler execute env:
                Map<String, Object> env = null;
                // add retry func
                int curRetry = 0;
                int maxRetry = 3;
                while (true) {
                    try {
                        env = getDolphinExecuteEnv(taskEvent, hostList);
                        break;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        message = "第[" + curRetry + "]次初始化dolphin环境变量失败，原因：" + e.getMessage();
                        logPersist(taskEvent, message);
                        if (curRetry >= maxRetry) {
                            throw e;
                        }
                        curRetry++;
                    }
                }

                // start dolphin pipeline
                DolpFailureStrategy failureStrategy = DolpFailureStrategy.getByValue(taskEvent.getEventEntity().getFailureStrategy());

                String schedInInstanceId = globalService.getDolphinSchedulerInteractService().startPipeline(
                        taskEvent.getEventEntity().getProjectCode(), taskEvent.getEventEntity().getPipelineCode(),
                        env, failureStrategy);
                log.info("start dolphin pipeline ok, schedInInstanceId is {}, event summary is {}",
                        schedInInstanceId, taskEvent.getSummary());
                logPersist(taskEvent, "start dolphin pipeline ok, schedInInstanceId is: " + schedInInstanceId);
                // 更新批次节点的事件列表中的 schedInInstanceId
                executionNodeEventService.updateBatchNodeEventSchedId(taskEvent.getFlowId(), taskEvent.getInstanceId(),
                        taskEvent.getEventEntity().getProjectCode(), taskEvent.getEventEntity().getPipelineCode(),
                        nodeIdList, schedInInstanceId);
                ThreadUtils.sleep(getMinLoopWait());
                return true;
            }
            return false;
        } finally {
            if (isPreAlignLock) {
                redissonLockSupport.unLock(preAlignLockKey);
            }
        }
    }

    private boolean waitAlignmentPeerEvents(TaskEvent taskEvent) throws TaskEventHandleException {
        while (!getCurrentAlignmentState(taskEvent)) {
            ThreadUtils.sleep(getMinLoopWait() + taskEvent.getRandom().nextInt(getMaxLoopStep()));
        }
        return true;
    }

    private boolean getCurrentAlignmentState(TaskEvent taskEvent) throws TaskEventHandleException {
        if (isFirstEvent(taskEvent)) {
            return true;
        }

        long instanceId = taskEvent.getInstanceId();
        long flowId = taskEvent.getFlowId();
        List<ExecutionNodeEntity> nodeEntityList = getCurrentBatchNodeList(flowId, instanceId);
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

    private boolean updateCurrentEventStatus(TaskEvent taskEvent, String schedInstanceId) throws TaskEventHandleException {
        int waitLoopTime = 0;
        boolean isFinish = false;
        while (!isFinish) {

            if (waitLoopTime % logMod() == 0) {
                if (tryUpdateBatchSameEventResult(taskEvent, schedInstanceId)) {
                    isFinish = true;
                }
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
        return false;
    }

    private List<ExecutionNodeEntity> getCurrentBatchNodeList(Long flowId, Long instanceId) {
        return executionNodeService.queryNodeListByInstanceId(flowId, instanceId);
    }

    protected boolean tryUpdateBatchSameEventResult(TaskEvent taskEvent, String schedInstanceId) {
        Boolean isHoldUpdateLock = false;
        String updateTaskDetailLock = getLockKey(taskEvent, RedisLockKey.BMR_DEPLOY_DOLPHIN_SCHEDULER_UPDATE_TASK_DETAIL_KEY);
        try {
            isHoldUpdateLock = redissonLockSupport.tryLock(updateTaskDetailLock, getMinLoopWait(), -1, TimeUnit.MILLISECONDS);

            if (!isHoldUpdateLock) {
                return false;
            }
            log.info("target event of {} holdUpdateLock", taskEvent.getSummary());
            while (!judgeBatchEventUpdateFinish(taskEvent, schedInstanceId)) {
                refreshEventFromDB(taskEvent);
                ThreadUtils.sleep(getMinLoopWait());
            }
            ThreadUtils.sleep(getMinLoopWait() + 100);
            log.info("target event of {} loop and wait batchEventUpdateFinish, will release lock.", taskEvent.getSummary());
            return true;
        } finally {
            if (isHoldUpdateLock) {
                redissonLockSupport.unLock(updateTaskDetailLock);
            }
        }
    }

    private boolean judgeBatchEventUpdateFinish(TaskEvent taskEvent, String schedInstanceId) {
        ExecutionInstanceDetail instanceDetail = globalService.getDolphinSchedulerInteractService()
                .querySchedInstanceTaskDetail(taskEvent.getEventEntity().getProjectCode(), schedInstanceId);
        if (Objects.isNull(instanceDetail)) {
            String msg = String.format("%s -- [scheduled instance id: %s dolphin scheduler  schedInstanceId: %s]: 未在 dolphin scheduler 查询到对应实例",
                    LocalDateFormatterUtils.getNowDefaultFmt(), taskEvent.getInstanceId(), schedInstanceId);
            log.warn(msg);
            return false;
        }
        if (!instanceDetail.isAlreadyExec()) {
            String msg = String.format("%s -- [scheduled instance id: %s dolphin scheduler  schedInstance Id: %s]: 对应 dolphin scheduler 实例尚未 ready",
                    LocalDateFormatterUtils.getNowDefaultFmt(), taskEvent.getInstanceId(), schedInstanceId);
            log.warn(msg);
            return false;
        }

        List<TaskInstanceDetail> taskInstanceList = instanceDetail.getTaskInstanceList();
        if (CollectionUtils.isEmpty(taskInstanceList)) {
            String msg = String.format("%s -- [scheduled instance id: %s dolphin scheduler schedInstance Id: %s]: task instance list 为空",
                    LocalDateFormatterUtils.getNowDefaultFmt(), taskEvent.getInstanceId(), schedInstanceId);
            log.warn(msg);
            return false;
        }
        TaskInstanceDetail targetTaskInstanceDetail = null;
        for (TaskInstanceDetail taskInstanceDetail : taskInstanceList) {
            String targetTaskCode = taskEvent.getEventEntity().getTaskCode();
            if (targetTaskCode.equals(taskInstanceDetail.getTaskCode())) {
                targetTaskInstanceDetail = taskInstanceDetail;
                break;
            }
        }

        DolphinWorkflowExecutionStatus workflowExecutionStatus = DolphinWorkflowExecutionStatus.valueOf(instanceDetail.getState());
        if (Objects.isNull(targetTaskInstanceDetail)) {
            // 快速失败
            // pipeline发生了改变，或者手动失败任务，快速退出执行
            if (DolphinWorkflowExecutionStatus.isFailure(workflowExecutionStatus)) {
                updateBatchEventStatue(taskEvent, schedInstanceId,
                        EventStatusEnum.FAIL_EVENT_EXECUTE, LocalDateTime.now(), 0l, 0l);
                return true;
            }
            return false;
        }
        DolpTaskType taskType = targetTaskInstanceDetail.getTaskType();
        switch (taskType) {
            case JOB_AGENT:
                JobAgentResultDO jobAgentResultDO = targetTaskInstanceDetail.getJobAgentResultDO();
                boolean invokerJobAgentSuccess;
                if (Objects.isNull(jobAgentResultDO)) {
                    DolphinWorkflowExecutionStatus taskInstanceState = targetTaskInstanceDetail.getState();
                    if (DolphinWorkflowExecutionStatus.isFailure(taskInstanceState)) {
                        updateBatchEventStatue(taskEvent, schedInstanceId,
                                EventStatusEnum.FAIL_EVENT_EXECUTE, LocalDateTime.now(), 0l, 0l);
                        return true;
                    }
                    return false;
                }
                invokerJobAgentSuccess = jobAgentResultDO.isSuccess();
                if (invokerJobAgentSuccess) {
                    long taskSetId = jobAgentResultDO.getData().getId();
                    if (taskSetId <= 0) {
                        return false;
                    }
                    TaskSetData taskSetSummary = globalService.getJobAgentService().getTaskSetSummary(taskSetId);
                    if (Objects.isNull(taskSetSummary)) {
                        return false;
                    }
                    List<TaskAtomDetail> taskList = globalService.getJobAgentService().getTaskList(taskSetId);
                    if (CollectionUtils.isEmpty(taskList)) {
                        return false;
                    }
                    boolean hostnameNotMatch = false;
                    int finishCnt = 0;

                    // query all peer event list
                    List<ExecutionNodeEventEntity> nodeEventEntityList = executionNodeEventService.queryEventListByFlowIdExecuteOrderInstanceId(taskEvent.getFlowId(), taskEvent.getInstanceId(), taskEvent.getExecuteOrder());
                    if (CollectionUtils.isEmpty(nodeEventEntityList)) {
                        log.info("try judgeBatchEventUpdateFinish, but not find any node events, may instanceId already change," +
                                        " flowId is {}, instanceId is {}, executeOrder is {}",
                                taskEvent.getFlowId(), taskEvent.getInstanceId(), taskEvent.getExecuteOrder());
                        return false;
                    }
                    Map<Long, ExecutionNodeEventEntity> nodeId2EventMap = nodeEventEntityList.stream()
                            .collect(Collectors.toMap(ExecutionNodeEventEntity::getExecutionNodeId, e -> e));

                    for (TaskAtomDetail taskAtomDetail : taskList) {
                        String hostname = taskAtomDetail.getHostname();
                        ExecutionNodeEntity nodeEntity = executionNodeService.queryByHostnameAndInstanceId(taskEvent.getFlowId(), hostname, taskEvent.getInstanceId());
                        if (Objects.isNull(nodeEntity)) {
                            List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryByHostname(taskEvent.getFlowId(), hostname);
                            if (CollectionUtils.isEmpty(nodeEntityList)) {
                                log.info("get nodeEntity by queryByHostname not exists, flowId is {}, real exec node name is {}, current node name is {}",
                                        taskEvent.getFlowId(), hostname, taskEvent.getNodeName());
                                hostnameNotMatch = true;
                                continue;
                            } else {
                                String errorMsg = String.format("try update node event, but find node and event instanceId already change, skipped... flowId is %s, instanceId is %s, executeOrder is %s",
                                        taskEvent.getFlowId(), taskEvent.getInstanceId(), taskEvent.getExecuteOrder());
                                throw new IllegalArgumentException(errorMsg);
                            }
                        }
                        EventStatusEnum eventStatus = JobAgentTaskState.transferToStatus(taskAtomDetail.getState());
                        LocalDateTime eventEndTime = null;
                        if (eventStatus.isFinish() && taskAtomDetail.getEndTime() > 0) {
                            finishCnt++;
                            eventEndTime = DateTimeUtils.timestampToLocalDateTime(taskAtomDetail.getEndTime() / 1_000_000);
                        }
                        long jobSetId = 0l;
                        long jobTaskId = 0l;
                        if (taskAtomDetail.getSetId() > 0) {
                            jobSetId = taskAtomDetail.getSetId();
                        }
                        if (taskAtomDetail.getId() > 0) {
                            jobTaskId = taskAtomDetail.getId();
                        }

                        // query db target event state
                        ExecutionNodeEventEntity dbEventEntity = nodeId2EventMap.get(nodeEntity.getId());
                        if (Objects.isNull(dbEventEntity)) {
                            log.info("not find target node event, may current instanceId {} already change, nodeName is {}, skip update",
                                    taskEvent.getInstanceId(), hostname);
                            continue;
                        }

                        boolean needUpdate = dbEventEntity.getEventStatus() != eventStatus;
                        if (needUpdate) {
//                            executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(),
//                                    nodeEntity.getId(), taskEvent.getEventEntity().getTaskCode(), schedInstanceId,
//                                    eventStatus, eventEndTime, jobSetId, jobTaskId);
                            // 更新event执行结果，通过eventId
                            executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(), dbEventEntity.getId(),
                                    eventStatus, eventEndTime, jobSetId, jobTaskId);
                        }
                    }
                    int batchState = taskSetSummary.getState();
                    if (JobAgentTaskState.isStateFinish(batchState)) {
                        log.info("target event of {} detect finish, taskSetSummary state is {}.",
                                taskEvent.getSummary(), JobAgentTaskState.getByCode(batchState));
                        if (!hostnameNotMatch) {
                            return true;
                        }
                        // 如果taskset的状态是执行完成，更新当前批次所有节点状态，主要解决主节点脚本执行的场景
                        EventStatusEnum eventStatus = null;
                        if (JobAgentTaskState.isSuccess(batchState)) {
                            eventStatus = EventStatusEnum.SUCCEED_EVENT_EXECUTE;
                        }
                        if (JobAgentTaskState.isFailed(batchState)) {
                            eventStatus = EventStatusEnum.FAIL_EVENT_EXECUTE;
                        }

                        // 确认是执行完成
                        if (!Objects.isNull(eventStatus)) {
                            updateBatchEventStatue(taskEvent, schedInstanceId,
                                    eventStatus, LocalDateTime.now(), taskSetSummary.getId(), 0l);
                            return true;
                        }
                    }
                    if (finishCnt >= taskList.size()) {
                        if (!hostnameNotMatch) {
                            return true;
                        }
                        // loop query until taskSetSummary has explicit summary state
                    }
                } else {
                    // job invoker failed
                    updateBatchEventStatue(taskEvent, schedInstanceId,
                            EventStatusEnum.FAIL_EVENT_EXECUTE, LocalDateTime.now(), 0l, 0l);
                    return true;
                }
                break;
            default:
                throw new IllegalArgumentException("un-support job type:" + taskType);
        }
        return false;
    }

    private void updateBatchEventStatue(TaskEvent taskEvent, String schedInstanceId,
                                        EventStatusEnum eventStatue, LocalDateTime endTime, long jobSetId, long jobTaskId) {
        List<ExecutionNodeEntity> nodeEntityList = executionNodeService.getAlignNodeListByEventStatus(
                taskEvent.getFlowId(), taskEvent.getInstanceId(),
                taskEvent.getExecuteOrder() - 1,
                Arrays.asList(EventStatusEnum.SUCCEED_EVENT_EXECUTE, EventStatusEnum.SKIPPED));
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return;
        }

        // 过滤出执行中的节点
        nodeEntityList.stream().filter(node -> {
            NodeExecuteStatusEnum nodeStatus = node.getNodeStatus();
            return nodeStatus.isInExecute();
        }).map(ExecutionNodeEntity::getId).forEach(nodeId ->
                executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(),
                        nodeId, taskEvent.getEventEntity().getTaskCode(), schedInstanceId,
                        eventStatue, endTime, jobSetId, jobTaskId));
    }

    /**
     * 设置dolphin的env方法
     *
     * @param taskEvent
     * @param hostList
     * @return
     */
    protected Map<String, Object> getDolphinExecuteEnv(TaskEvent taskEvent, List<String> hostList) {
        ExecutionFlowEntity flowEntity = executionFlowService.getById(taskEvent.getFlowId());

        Long componentId = getComponentId(taskEvent, flowEntity);
        BmrMetadataService bmrMetadataService = globalService.getBmrMetadataService();
        MetadataComponentData component = bmrMetadataService.queryComponentByComponentId(componentId);
        String componentName = component.getComponentName();


        Preconditions.checkNotNull(flowEntity, "flow not exists");
        Map<String, Object> instanceEnv = new LinkedHashMap<>();
        instanceEnv.put(Constants.COMPONENT_ROLE, flowEntity.getRoleName());
        instanceEnv.put(Constants.COMPONENT_CLUSTER, flowEntity.getClusterName());
        instanceEnv.put(Constants.FLOW_ID, flowEntity.getId());
        instanceEnv.put(Constants._JOB_EXCUTE_TYPE, flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.RELASE_SCOPE, flowEntity.getReleaseScopeType());
        instanceEnv.put(Constants.BATCH_ID, taskEvent.getBatchId());
        log.info("JOB_EXECUTE_TYPE :" + flowEntity.getJobExecuteType());
        instanceEnv.put(Constants.COMPONENT_NAME, componentName);
        String downloadDir = Constants.DOWNLOAD_DIR_VALUE + componentName + File.separator
                + componentId;
        instanceEnv.put(Constants.DOWNLOAD_DIR, downloadDir);
        log.info("download dir is ====== {}", downloadDir);
        instanceEnv.put(Constants.DEPLOYMENT_ORDER_CREATOR, flowEntity.getOperator());

        instanceEnv.put(Constants.NODE_WARNINGS_NUMBER, component.getNodeWarningsNumber());

        StringJoiner joiner = new StringJoiner(Constants.COMMA);
        hostList.forEach(joiner::add);
        // 机器列表
        instanceEnv.put(Constants.SYSTEM_JOBAGENT_EXEC_HOSTS, joiner.toString());
        // 服务是否重启
        instanceEnv.put(Constants.SERVICE_RESTART, flowEntity.getRestart());
        // 生效方式
        instanceEnv.put(Constants.EFFECTIVE_MODE, flowEntity.getEffectiveMode());
        Long packageId = getPackageId(taskEvent, flowEntity);

        if (NumberUtils.isPositiveLong(packageId)) {
            MetadataPackageData ciPackMetadataEntity = bmrMetadataService.queryPackageDetailById(packageId);
            instanceEnv.put(Constants.CI_PACK_ID, ciPackMetadataEntity.getId());
            instanceEnv.put(Constants.CI_PACK_MD5, ciPackMetadataEntity.getProductBagMd5());
            instanceEnv.put(Constants.CI_PACK_NAME, ciPackMetadataEntity.getProductBagName());
            instanceEnv.put(Constants.CI_PACK_TAG_NAME, ciPackMetadataEntity.getTagName());
            String packageDownloadUrl = bmrMetadataService.queryPackageDownloadInfo(packageId);
            instanceEnv.put(Constants.CI_PACK_URL, packageDownloadUrl);
        } else {
            instanceEnv.put(Constants.CI_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CI_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CI_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CI_PACK_TAG_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CI_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CI_PACK_URL, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.HOST_ENV_MAP_KEY, JSONUtil.toJsonStr(Collections.EMPTY_MAP));
        }

        //  添加组件变量
        Map<String, String> componentVariableMap = bmrMetadataService.queryVariableByComponentId(componentId);
        for (Map.Entry<String, String> entry : componentVariableMap.entrySet()) {
            instanceEnv.put(entry.getKey(), entry.getValue());
        }

        Map<String, HostAndLogicGroupInfo> hostGroupInfos = globalService.getBmrResourceService().queryNodeGroupInfo(flowEntity.getClusterId(), hostList);
        log.info("query node group info is {}.", JSONUtil.toJsonStr(hostGroupInfos));

        Long configId = getConfigId(taskEvent, flowEntity);
        if (NumberUtils.isPositiveLong(configId)) {
            ConfigDetailData configMetadataEntity = globalService.getBmrConfigService().queryConfigDetailById(configId);
            instanceEnv.put(Constants.CONFIG_PACK_ID, configMetadataEntity.getId());
            instanceEnv.put(Constants.CONFIG_PACK_MD5, configMetadataEntity.getConfigVersionMd5());
            instanceEnv.put(Constants.CONFIG_PACK_NAME, configMetadataEntity.getConfigVersionNumber() + ".zip");
            instanceEnv.put(Constants.CONFIG_PACK_VERSION, configMetadataEntity.getConfigVersionNumber());
            String configDownloadUrl = configMetadataEntity.getDownloadUrl();
            instanceEnv.put(Constants.CONFIG_PACK_URL, configDownloadUrl);
            // 设置节点组信息
            //  当前以运行时的节点分组为准

            List<ConfigGroupDo> configGroupDoList = globalService.getBmrConfigService().queryConfigGroupInfoById(configId);
            ConfigGroupDo defaultConfigGroupDo = null;
            HashMap<Integer, ConfigGroupDo> configGroupMap = new HashMap<>();
            for (ConfigGroupDo configGroupDo : configGroupDoList) {
                configGroupMap.put(configGroupDo.getLogicGroupId(), configGroupDo);
                if (configGroupDo.isDefaultGroup()) {
                    defaultConfigGroupDo = configGroupDo;
                }
            }

            Map<String, Map<String, String>> hostEnvMap = new LinkedHashMap<>();
            for (Map.Entry<String, HostAndLogicGroupInfo> entry : hostGroupInfos.entrySet()) {
                String hostname = entry.getKey();
                HostAndLogicGroupInfo hostAndLogicGroupInfo = entry.getValue();
                int group = hostAndLogicGroupInfo.getLogicGroupId().intValue();
                HashMap<String, String> hostEnv = new HashMap<>();
                if (configGroupMap.containsKey(group)) {
                    hostEnv.put(Constants.CONFIG_NODE_GROUP, configGroupMap.get(group).getDirName());
                } else {
                    Preconditions.checkNotNull(defaultConfigGroupDo, "配置中心默认分组不存在，请检查");
                    hostEnv.put(Constants.CONFIG_NODE_GROUP, defaultConfigGroupDo.getDirName());
                }
                // 设置主机对应的分组名称
                hostEnv.put(Constants.NODE_GROUP_NAME, hostAndLogicGroupInfo.getLogicGroupName());

                hostEnvMap.put(hostname, hostEnv);

                hostEnv.put(Constants.NUM_SSD, String.valueOf(hostAndLogicGroupInfo.getNumSsd()));
                hostEnv.put(Constants.NUM_SATA, String.valueOf(hostAndLogicGroupInfo.getNumSata()));
                hostEnv.put(Constants.NUM_NVME, String.valueOf(hostAndLogicGroupInfo.getNvmeTotal()));
            }
            instanceEnv.put(Constants.HOST_ENV_MAP_KEY, JSONUtil.toJsonStr(hostEnvMap));

        } else {
            instanceEnv.put(Constants.CONFIG_PACK_ID, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CONFIG_PACK_MD5, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CONFIG_PACK_NAME, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CONFIG_PACK_VERSION, Constants.EMPTY_STRING);
            instanceEnv.put(Constants.CONFIG_PACK_URL, Constants.EMPTY_STRING);

            Map<String, Map<String, String>> hostEnvMap = new LinkedHashMap<>();
            for (Map.Entry<String, HostAndLogicGroupInfo> entry : hostGroupInfos.entrySet()) {
                String hostname = entry.getKey();
                HostAndLogicGroupInfo hostAndLogicGroupInfo = entry.getValue();
                HashMap<String, String> hostEnv = new HashMap<>();
                // 设置主机对应的分组名称
                hostEnv.put(Constants.NODE_GROUP_NAME, hostAndLogicGroupInfo.getLogicGroupName());

                hostEnvMap.put(hostname, hostEnv);

                hostEnv.put(Constants.NUM_SSD, String.valueOf(hostAndLogicGroupInfo.getNumSsd()));
                hostEnv.put(Constants.NUM_SATA, String.valueOf(hostAndLogicGroupInfo.getNumSata()));
                hostEnv.put(Constants.NUM_NVME, String.valueOf(hostAndLogicGroupInfo.getNvmeTotal()));
            }
            instanceEnv.put(Constants.HOST_ENV_MAP_KEY, JSONUtil.toJsonStr(hostEnvMap));
        }
        dolphinEnvExtendedService.fillExtendedEnv(taskEvent, instanceEnv);
        return instanceEnv;
    }

    protected Long getConfigId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        Long configId = flowEntity.getConfigId();
        return configId;
    }

    protected Long getPackageId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        Long packageId = flowEntity.getPackageId();
        return packageId;
    }

    protected Long getComponentId(TaskEvent taskEvent, ExecutionFlowEntity flowEntity) {
        Long componentId = flowEntity.getComponentId();
        return componentId;
    }

    protected int getMinLoopWait() {
        return MIN_LOOP_WAIT_TIME;
    }

    protected int getMaxLoopStep() {
        return MAX_LOOP_WAIT_TIME;
    }

    protected int logMod() {
        return LOG_LOOP_MOD;
    }

    public String getInvokerToken() {
        return deployToken;
    }

}
