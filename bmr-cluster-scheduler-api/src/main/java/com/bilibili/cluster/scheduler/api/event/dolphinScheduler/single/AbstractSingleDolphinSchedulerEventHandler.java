package com.bilibili.cluster.scheduler.api.event.dolphinScheduler.single;

import com.bilibili.cluster.scheduler.api.event.dolphinScheduler.AbstractDolphinSchedulerEventHandler;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskAtomDetail;
import com.bilibili.cluster.scheduler.common.dto.jobAgent.TaskSetData;
import com.bilibili.cluster.scheduler.common.dto.scheduler.ExecutionInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.TaskInstanceDetail;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.JobAgentResultDO;
import com.bilibili.cluster.scheduler.common.enums.dolphin.DolphinWorkflowExecutionStatus;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.jobAgent.JobAgentTaskState;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpFailureStrategy;
import com.bilibili.cluster.scheduler.common.enums.scheduler.DolpTaskType;
import com.bilibili.cluster.scheduler.common.event.TaskEvent;
import com.bilibili.cluster.scheduler.common.utils.DateTimeUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
public abstract class AbstractSingleDolphinSchedulerEventHandler extends AbstractDolphinSchedulerEventHandler {

    @Override
    public boolean trySubmitDolphinPipeline(TaskEvent taskEvent) throws Exception {
        // double check already submit or not
        refreshEventFromDB(taskEvent);
        String schedInstanceId = taskEvent.getEventEntity().getSchedInstanceId();
        if (!StringUtils.isBlank(schedInstanceId)) {
            return true;
        }

        String message = taskEvent.getNodeName() + ": current event is singleNode DolphinSchedulerEvent, will start dolphin pipeline execute ...";
        log.info(message);
        logPersist(taskEvent, message);

        List<String> hostList = new ArrayList<>();
        hostList.add(taskEvent.getNodeName());

        List<String> lostJobAgentList = globalService.getJobAgentService().queryLostJobAgent(hostList);
        if (!CollectionUtils.isEmpty(lostJobAgentList)) {
           throw new IllegalArgumentException("job-agent is lost");
        }

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
                Arrays.asList(taskEvent.getExecutionNodeId()), schedInInstanceId);
        ThreadUtils.sleep(getMinLoopWait());
        return true;
    }


    protected boolean tryUpdateBatchSameEventResult(TaskEvent taskEvent, String schedInstanceId) {
        while (!judgeCurrentEventUpdateFinish(taskEvent, schedInstanceId)) {
            refreshEventFromDB(taskEvent);
            ThreadUtils.sleep(getMinLoopWait());
        }
        log.info("target event of {} loop and wait batchEventUpdateFinish, will release lock.", taskEvent.getSummary());
        return true;
    }

    protected boolean judgeCurrentEventUpdateFinish(TaskEvent taskEvent, String schedInstanceId) {

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
                updateSingleEventStatue(taskEvent, schedInstanceId,
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
                        updateSingleEventStatue(taskEvent, schedInstanceId,
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

                    for (TaskAtomDetail taskAtomDetail : taskList) {
                        String hostname = taskAtomDetail.getHostname();
                        if (!taskEvent.getNodeName().equals(hostname)) {
                            hostnameNotMatch = true;
                            break;
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

                        boolean needUpdate = taskEvent.getEventStatus() != eventStatus;
                        if (needUpdate) {
                            executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(),
                                    taskEvent.getExecutionNodeId(), taskEvent.getEventEntity().getTaskCode(), schedInstanceId,
                                    eventStatus, eventEndTime, jobSetId, jobTaskId);
                            taskEvent.setEventStatus(eventStatus);
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
                            updateSingleEventStatue(taskEvent, schedInstanceId,
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
                    updateSingleEventStatue(taskEvent, schedInstanceId,
                            EventStatusEnum.FAIL_EVENT_EXECUTE, LocalDateTime.now(), 0l, 0l);
                    return true;
                }
                break;
            default:
                throw new IllegalArgumentException("un-support job type:" + taskType);
        }
        return false;
    }

    public boolean updateSingleEventStatue(TaskEvent taskEvent, String schedInstanceId,
                                           EventStatusEnum eventStatue, LocalDateTime endTime, long jobSetId, long jobTaskId) {
        executionNodeEventService.updateEventExecDate(taskEvent.getFlowId(), taskEvent.getInstanceId(),
                taskEvent.getExecutionNodeId(), taskEvent.getEventEntity().getTaskCode(), schedInstanceId,
                eventStatue, endTime, jobSetId, jobTaskId);
        return true;
    }

}
