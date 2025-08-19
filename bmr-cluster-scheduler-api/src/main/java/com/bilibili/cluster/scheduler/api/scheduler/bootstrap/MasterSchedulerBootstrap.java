package com.bilibili.cluster.scheduler.api.scheduler.bootstrap;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.enums.SlotCheckState;
import com.bilibili.cluster.scheduler.api.exceptions.MasterException;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceEvent;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceQueue;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceType;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowStartInstanceTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.registry.service.impl.ServerNodeManager;
import com.bilibili.cluster.scheduler.api.scheduler.cache.ProcessInstanceExecCacheManager;
import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceExecuteRunnable;
import com.bilibili.cluster.scheduler.api.scheduler.runner.WorkflowInstanceLooper;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import com.bilibili.cluster.scheduler.common.thread.BaseDaemonThread;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Master scheduler thread, this thread will consume the commands from database and trigger processInstance executed.
 */
@Service
public class MasterSchedulerBootstrap extends BaseDaemonThread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(MasterSchedulerBootstrap.class);

    @Resource
    private MasterConfig masterConfig;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private ExecutionNodeService executionNodeService;

    /**
     * master prepare exec service
     */
    private ThreadPoolExecutor masterPrepareExecService;

    @Resource
    private WorkflowInstanceLooper workflowInstanceLooper;

    @Resource
    private WorkflowStartInstanceTaskEventHandler workflowStartInstanceTaskEventHandler;

    @Resource
    private ProcessInstanceExecCacheManager processInstanceExecCacheManager;

    @Resource
    private WorkflowInstanceQueue workflowInstanceQueue;

    @Resource
    ExecutionLogService executionLogService;

    private String masterAddress;

    protected MasterSchedulerBootstrap() {
        super("MasterWorkerFlowLoopThread");
    }

    /**
     * constructor of MasterSchedulerService
     */
    public void init() {
        this.masterPrepareExecService = (ThreadPoolExecutor) ThreadUtils
                .newDaemonFixedThreadExecutor("MasterPreExecThread", masterConfig.getPreExecThreads());
        this.masterAddress = NetUtils.getAddr(masterConfig.getListenPort());
    }

    @Override
    public synchronized void start() {
        logger.info("Master schedule work flow process bootstrap starting..");
        super.start();
        workflowInstanceLooper.start();
        logger.info("Master schedule work flow process bootstrap started...");
    }

    @Override
    public void close() {
        logger.info("Master schedule bootstrap stopping...");
        logger.info("Master schedule bootstrap stopped...");
    }

    /**
     * run of MasterSchedulerService
     */
    @Override
    public void run() {
        while (!ServerLifeCycleManager.isStopped()) {
            try {
                if (!ServerLifeCycleManager.isRunning()) {
                    // the current server is not at running status, cannot consume command.
                    Thread.sleep(Constants.SLEEP_TIME_MILLIS);
                }

                List<ExecutionFlowEntity> executionFlowList = findExecutionFlowProcess();
                logger.info("Master executionFlowList size: {}", executionFlowList.size());
                if (CollectionUtils.isEmpty(executionFlowList)) {
                    // indicate that no command ,sleep for 1s
                    Thread.sleep(Constants.SLEEP_TIME_MILLIS);
                    continue;
                }

                List<ExecutionFlowInstanceDTO> workFlowProcessInstances = workFlowProcess2ProcessInstance(executionFlowList);
                logger.info("Master executionFlowList size: {} ,  workFlowProcessInstances size: {}", executionFlowList.size(), workFlowProcessInstances.size());

                if (CollectionUtils.isEmpty(workFlowProcessInstances)) {
                    // indicate that the command transform to processInstance error, sleep for 1s
                    Thread.sleep(Constants.SLEEP_TIME_MILLIS);
                    continue;
                }

                workFlowProcessInstances.forEach(workFlowProcessInstance -> {
                    try {
                        Long instanceId = workFlowProcessInstance.getInstanceId();
                        Long flowId = workFlowProcessInstance.getFlowId();
                        MDC.put(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY, String.valueOf(instanceId));
                        // 生成待处理的调度实例
                        WorkflowInstanceExecuteRunnable workflowInstanceExecuteRunnable =
                                new WorkflowInstanceExecuteRunnable(
                                        workFlowProcessInstance,
                                        workflowStartInstanceTaskEventHandler,
                                        executionNodeService,
                                        masterAddress);
                        processInstanceExecCacheManager.cache(instanceId, workflowInstanceExecuteRunnable);
                        WorkflowInstanceEvent workflowInstanceEvent = new WorkflowInstanceEvent(WorkflowInstanceType.START_WORKFLOW,
                                instanceId,
                                flowId);
                        workflowInstanceQueue.addEvent(workflowInstanceEvent);
                        logger.info("Master finish add workflowInstanceEvent: {}", JSONUtil.toJsonStr(workflowInstanceEvent));

                        String logMsg = String.format("Master finish add work flow instanceEvent");
                        logger.info(logMsg);
                        executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

                    } finally {
                        MDC.remove(Constants.WORK_FLOW_PROCESS_INSTANCE_ID_MDC_KEY);
                    }
                });
            } catch (InterruptedException interruptedException) {
                logger.warn("Master schedule bootstrap interrupted, close the loop", interruptedException);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Master schedule workflow error", e);
                // sleep for 1s here to avoid the database down cause the exception boom
                ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
            }
        }
    }


    private List<ExecutionFlowInstanceDTO> workFlowProcess2ProcessInstance(List<ExecutionFlowEntity> executionFlowList) throws InterruptedException {
        logger.info(
                "Master schedule bootstrap transforming ExecutionFlow to ExecutionFlowInstance, ExecutionFlow size: {}",
                executionFlowList.size());
        List<ExecutionFlowInstanceDTO> workFlowProcessInstances =
                Collections.synchronizedList(new ArrayList<>(executionFlowList.size()));
        CountDownLatch latch = new CountDownLatch(executionFlowList.size());
        for (final ExecutionFlowEntity executionFlowEntity : executionFlowList) {
            masterPrepareExecService.execute(() -> {
                try {
                    SlotCheckState slotCheckState = slotCheck(executionFlowEntity);
                    if (slotCheckState.equals(SlotCheckState.CHANGE) || slotCheckState.equals(SlotCheckState.INJECT)) {
                        logger.info("Master handle WorkFlowProcess {} skip, slot check state: {}",
                                executionFlowEntity.getId(), slotCheckState);
                        return;
                    }

                    String logMsg = String.format("start execute flow flowId: %s", executionFlowEntity.getId());
                    executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, logMsg);

                    ExecutionFlowInstanceDTO workFlowProcessInstance =
                            executionFlowService.handleWorkFlowProcess(masterAddress, executionFlowEntity);
                    if (workFlowProcessInstance != null) {
                        workFlowProcessInstances.add(workFlowProcessInstance);
                        logger.info("Master handle work process {} end, create process instance {}",
                                executionFlowEntity.getId(),
                                JSONUtil.toJsonStr(workFlowProcessInstance));
                    }
                } catch (Exception e) {
                    logger.error("Master handle WorkFlowProcess {} error ", executionFlowEntity.getId(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // make sure to finish handling command each time before next scan
        latch.await();
//        logger.info(
//                "Master schedule bootstrap transformed to work flow task event, workFlowProcesses size: {}, taskEventSize: {}",
//                executionFlowList.size(), workFlowProcessInstances.size());
        return workFlowProcessInstances;
    }

    private List<ExecutionFlowEntity> findExecutionFlowProcess() throws MasterException {
        List<ExecutionFlowEntity> flowList = new ArrayList<>();
        int pageNumber = 0;
        int pageSize = masterConfig.getFetchCommandNum();
        try {
            while(true) {
                int thisMasterSlot = ServerNodeManager.getSlot();
                int masterCount = ServerNodeManager.getMasterSize();
                if (masterCount <= 0) {
                    logger.warn("Master count: {} is invalid, the current slot: {}", masterCount, thisMasterSlot);
                    return Collections.emptyList();
                }

                final List<ExecutionFlowEntity> result =
                        executionFlowService.findExecuteFlowPageBySlot(pageSize, pageNumber, masterCount,
                                thisMasterSlot);
                if (CollectionUtils.isNotEmpty(result)) {
                    logger.info(
                            "Master schedule bootstrap loop command success, command size: {}, current slot: {}, total slot size: {}",
                            result.size(), thisMasterSlot, masterCount);
                    flowList.addAll(result);
                    pageNumber++;
                } else {
                    break;
                }
            }
            return flowList;
        } catch (Exception ex) {
            throw new MasterException("Master loop command from database error", ex);
        }
    }

    private SlotCheckState slotCheck(ExecutionFlowEntity executionFlowEntity) {
        int slot = ServerNodeManager.getSlot();
        int masterSize = ServerNodeManager.getMasterSize();
        SlotCheckState state;
        if (masterSize <= 0) {
            state = SlotCheckState.CHANGE;
        } else if (executionFlowEntity.getId() % masterSize == slot) {
            state = SlotCheckState.PASS;
        } else {
            state = SlotCheckState.INJECT;
        }
        return state;
    }

}
