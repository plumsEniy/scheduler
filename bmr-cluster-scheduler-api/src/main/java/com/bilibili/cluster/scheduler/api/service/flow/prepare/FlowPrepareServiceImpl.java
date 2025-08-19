package com.bilibili.cluster.scheduler.api.service.flow.prepare;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.registry.service.impl.ServerNodeManager;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowAopEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.bus.FlowPrepareGenerateFactoryBusService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.lifecycle.ServerLifeCycleManager;
import com.bilibili.cluster.scheduler.common.thread.BaseDaemonThread;
import com.bilibili.cluster.scheduler.common.utils.ThreadUtils;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FlowPrepareServiceImpl implements FlowPrepareService, InitializingBean {

    // 待处理flow列表
    private LinkedBlockingQueue<Long> waitPrepareFlowQueue = new LinkedBlockingQueue<>();

    private FlowPrepareTask prepareTask;

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionLogService logService;

    @Resource
    FlowPrepareGenerateFactoryBusService flowPrepareGenerateFactoryBusService;

    @Resource
    RedissonLockSupport redissonLockSupport;

    @Resource
    BmrFlowService bmrFlowService;

    @Resource
    ExecutionFlowAopEventService executionFlowAopEventService;

    @Override
    public void prepareFlowExecuteNodeAndEvents(ExecutionFlowEntity flowEntity) {
        Preconditions.checkState(flowEntity.getFlowStatus() == FlowStatusEnum.PREPARE_EXECUTE,
                "only flow status of 'PREPARE_EXECUTE' can prepareFlowExecuteNodeAndEvents, flow is: "
                        + JSONUtil.toJsonStr(flowEntity));
        FlowDeployType deployType = flowEntity.getDeployType();
        Long flowId = flowEntity.getId();
        String msg;
        try {
            logService.updateLogContent(flowId, LogTypeEnum.FLOW, "start generateNodeAndEvents of flowId: " + flowId);
            // deployFactoryHolder.get(deployType).generateNodeAndEvents(flowEntity);
            flowPrepareGenerateFactoryBusService.forwardFlowPrepareGenerateFactory(flowEntity)
                    .generateNodeAndEvents(flowEntity);


            /**
             * 更新最大批次id
             */
            Integer maxBatchId = executionNodeService.queryMaxBatchId(flowId);
            if (maxBatchId.intValue() > 0) {
                flowService.updateMaxBatchId(flowId, maxBatchId);
                msg = flowId + " prepare ok, will switch to 'UN_EXECUTE' status";
                flowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.UN_EXECUTE);
                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                updateBmrFlowDto.setFlowId(flowId);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.PROCEED_ALL);
                updateBmrFlowDto.setStartTime(LocalDateTime.now());
                updateBmrFlowDto.setFlowStatus(BmrFlowStatus.RUNNING);
                updateBmrFlowDto.setApplyState("APPROVED");
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                executionFlowAopEventService.startFlowAop(flowEntity);
            } else {
                msg = flowId + " prepare ok, not find any nodes, skip execute...";
                flowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.TERMINATE);

                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                updateBmrFlowDto.setFlowId(flowId);
                updateBmrFlowDto.setFlowStatus(BmrFlowStatus.SUCCESS);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.DONE_BUT_NOT_FINISH);
                updateBmrFlowDto.setEndTime(LocalDateTime.now());
                updateBmrFlowDto.setApplyState("APPROVED");
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                executionFlowAopEventService.finishFlowAop(flowEntity, FlowStatusEnum.FAIL_EXECUTE);
            }
            log.info(msg);
        } catch (Exception e) {
            msg = flowId + " prepare failed, will switch to 'PREPARE_EXECUTE_FAILED' status, case by:" + e.getMessage();
            log.error(msg, e);
            flowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.PREPARE_EXECUTE_FAILED);

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            updateBmrFlowDto.setFlowId(flowId);
            updateBmrFlowDto.setFlowStatus(BmrFlowStatus.RUNNING);
            updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.FAILED_PAUSE);
            updateBmrFlowDto.setEndTime(LocalDateTime.now());
            updateBmrFlowDto.setApplyState("APPROVED");
            bmrFlowService.alterFlowStatus(updateBmrFlowDto);
            executionFlowAopEventService.giveUpFlowAop(flowEntity);
        }
        logService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(DeployOneFlowReq req,
                                                        ExecutionFlowEntity flowEntity,
                                                        @Nullable MetadataClusterData clusterDetail,
                                                        @Nullable MetadataComponentData componentDetail) throws Exception {
        final PipelineParameter pipelineParameter = new PipelineParameter(req, flowEntity, clusterDetail, componentDetail);
        return flowPrepareGenerateFactoryBusService.forwardFlowPrepareGenerateFactory(pipelineParameter)
                .resolvePipelineEventList(pipelineParameter);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        prepareTask = new FlowPrepareTask("FlowPrepareTask");
        prepareTask.start();
        log.info("FlowPrepareTask start ok");
    }


    private class FlowPrepareTask extends BaseDaemonThread {

        private final Logger logger = LoggerFactory.getLogger(FlowPrepareTask.class);

        protected FlowPrepareTask(String threadName) {
            super(threadName);
        }

        @Override
        public synchronized void start() {
            logger.info("Flow Prepare task staring");
            super.start();
            logger.info("Flow Prepare task stared");
        }

        @Override
        public void run() {
            MDC.put(Constants.LOG_TRACE_ID, IdUtil.simpleUUID());
            // when startup, wait 30s for ready
            logger.info("when flowPrepareService startup, wait 30s for task ready");
            ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS * 3);

            while (!ServerLifeCycleManager.isStopped()) {
                try {
                    if (!ServerLifeCycleManager.isRunning()) {
                        continue;
                    }
//                    final StopWatch watch = new StopWatch("FlowPrepareTask-consumerPrepareFlow-StopWatch");
//                    watch.start();
                    consumerPrepareFlow();
//                    watch.stop();
//                    log.info("FlowPrepareTask : stop watch of consumerPrepareFlow: \n" + watch.prettyPrint(TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    logger.error("Master failover thread execute error", e);
                } finally {
                    ThreadUtils.sleep(Constants.SLEEP_TIME_MILLIS);
                }
            }
        }

        private void consumerPrepareFlow() throws Exception {
            String lock = RedisLockKey.BMR_DEPLOY_SCHEDULER_PREPARE_ONE_FLOW_LOCK_KEY.name() + "-" + SpringApplicationContext.getEnv();
            boolean isLock = false;
            try {
                isLock = redissonLockSupport.tryLock(lock, Constants.ONE_SECOND * 3, -1, TimeUnit.MILLISECONDS);
                if (!isLock) {
                    return;
                }
//                long waitTs = Constants.ONE_SECOND * 3;
//                log.info("current task holder the lock {}, wait ts {}, than start running....", lock, waitTs);
//                ThreadUtil.safeSleep(waitTs);
                log.info("current task holder the lock {}, start running....", lock);
                waitPrepareFlowQueue.clear();

                List<Long> alreadyWaitFlowList = new ArrayList<>();
                // alreadyWaitFlowList.addAll(waitPrepareFlowQueue);
                List<ExecutionFlowEntity> anotherPrepareFlowList = flowService.queryPrepareFlowList(alreadyWaitFlowList);

                if (!CollectionUtils.isEmpty(anotherPrepareFlowList)) {
                    int thisMasterSlot = ServerNodeManager.getSlot();
                    int masterCount = ServerNodeManager.getMasterSize();
                    anotherPrepareFlowList.forEach(e -> {
                        long flowId = e.getId();
                        if (masterCount > 0 && flowId % masterCount == thisMasterSlot) {
                            waitPrepareFlowQueue.add(flowId);
                        }
                    });
                }
                log.info("require prepare flow list is {}", waitPrepareFlowQueue);

                while (!waitPrepareFlowQueue.isEmpty()) {
                    Long flowId = waitPrepareFlowQueue.take();
                    ExecutionFlowEntity flowEntity = flowService.queryByIdWithTransactional(flowId);
                    FlowStatusEnum flowStatus = flowEntity.getFlowStatus();

                    switch (flowStatus) {
                        case PREPARE_EXECUTE:
                            log.info("start prepare handle flowId {}", flowId);
                            logService.updateLogContent(flowId, LogTypeEnum.FLOW,
                                    flowId + ", start dispatcher prepare status flow for exec nodes and events initialisation");
                            prepareFlowExecuteNodeAndEvents(flowEntity);
                            break;
                        default:
                            logger.warn("flowId of {} status is {}, skip prepare flow...", flowId, flowStatus);
                    }
                }
            } finally {
                if (isLock) {
                    log.info("current task will release the lock {}", lock);
                    redissonLockSupport.unLock(lock);
                }
            }
        }
    }

}
