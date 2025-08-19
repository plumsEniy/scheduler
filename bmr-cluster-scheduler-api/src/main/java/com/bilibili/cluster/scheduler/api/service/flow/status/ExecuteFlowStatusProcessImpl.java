package com.bilibili.cluster.scheduler.api.service.flow.status;

import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.enums.RedisLockKey;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.scheduler.cache.ProcessInstanceExecCacheManager;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionLogService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.bus.FlowRollbackBusFactoryService;
import com.bilibili.cluster.scheduler.api.service.flow.*;
import com.bilibili.cluster.scheduler.api.service.oa.OAService;
import com.bilibili.cluster.scheduler.api.service.wx.WxPublisherService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.dto.oa.OAForm;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.oa.OAFormStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExecuteFlowStatusProcessImpl implements ExecuteFlowStatusProcess {

    @Resource
    private RedissonLockSupport redissonLockSupport;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private ExecutionNodeService executionNodeService;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    private ProcessInstanceExecCacheManager processInstanceExecCacheManager;

    @Resource
    private WxPublisherService wxPublisherService;

    @Resource
    private OAService oaService;

    @Resource
    ExecutionLogService executionLogService;

    @Value("${spring.profiles.active}")
    private String active;

    @Resource
    BmrFlowService bmrFlowService;

    @Resource
    BmrResourceService bmrResourceService;

    @Resource
    FlowRollbackBusFactoryService rollbackBusFactoryService;

    @Resource
    ExecutionFlowAopEventService executionFlowAopEventService;

    @Override
    @Scheduled(cron = "0/10 * * * * ?")
    public void updateFlowStatus() {
        boolean isLock = false;
        String lockKey = RedisLockKey.GLOBAL_UPDATE_SCHEDULER_EXECUTE_FLOW_STATUS_LOCK_KEY.name() + "-" + SpringApplicationContext.getEnv();
        try {
            isLock = redissonLockSupport.tryLock(lockKey, 1, -1, TimeUnit.SECONDS);
            if (!isLock) {
                log.warn("update flow status，not acquire lock, key:{}", lockKey);
                return;
            }
            log.info("start update flow status");
            // 1、查询待更新的flow状态
            List<FlowStatusEnum> handleFlowStatus = getRequireHandleFlowStatus();
            List<ExecutionFlowEntity> executionFlowEntities = executionFlowService.findExecuteFlowByFlowStatus(handleFlowStatus);
            // 更新flow状态
            updateFlowStatus(executionFlowEntities);

//            2、更新审批状态
            List<FlowStatusEnum> underApprovalStatusList = new ArrayList<>();
            underApprovalStatusList.add(FlowStatusEnum.UNDER_APPROVAL);
            List<ExecutionFlowEntity> underApprovalFlowList = executionFlowService.findExecuteFlowByFlowStatus(underApprovalStatusList);
            checkAndUpdateFlowApprovalStatus(underApprovalFlowList);

//          3、尝试自动接单（已完成的任务单）
            List<FlowStatusEnum> finishedStatusList = new ArrayList<>();
            finishedStatusList.add(FlowStatusEnum.SUCCEED_EXECUTE);
            List<ExecutionFlowEntity> finishedFlowList = executionFlowService.findExecuteFlowByFlowStatus(finishedStatusList);
            tryTerminateFlowIfNeed(finishedFlowList);

            Thread.sleep(1_000);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (isLock) {
                redissonLockSupport.unLock(lockKey);
            }
        }
    }

    private void tryTerminateFlowIfNeed(List<ExecutionFlowEntity> finishedFlowList) {
        if (CollectionUtils.isEmpty(finishedFlowList)) {
            return;
        }
        for (ExecutionFlowEntity flowEntity : finishedFlowList) {
            tryTerminateFlow(flowEntity);
        }
    }

    private void tryTerminateFlow(ExecutionFlowEntity flowEntity) {
        long flowId = flowEntity.getId();
        try {
            final FlowStatusEnum flowStatus = flowEntity.getFlowStatus();
            if (flowStatus != FlowStatusEnum.SUCCEED_EXECUTE) {
                return;
            }

            final FlowDeployType deployType = flowEntity.getDeployType();
            if (!deployType.isAutoClose()) {
                return;
            }

            final LocalDateTime endTime = flowEntity.getEndTime();
            if (Objects.isNull(endTime)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            if (Duration.between(endTime, now).toMillis() < TimeUnit.HOURS.toMillis(1)) {
                return;
            }

//            List<NodeExecuteStatusEnum> requireHandleNodeExecuteStatus = getRequireHandleNodeExecuteStatus();
//            // 存在失败任务不自动结单
//            requireHandleNodeExecuteStatus.add(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE);
//            requireHandleNodeExecuteStatus.add(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE);
//            requireHandleNodeExecuteStatus.add(NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE);
//
//            List<ExecutionNodeEntity> notFinishNodeList = executionNodeService.findExecuteNodeByNodeStatus(requireHandleNodeExecuteStatus);
//            if (!CollectionUtils.isEmpty(notFinishNodeList)) {
//                return;
//            }

            List<ExecutionNodeSummary> executionNodeSummaryList = executionNodeService.queryExecutionNodeSummary(flowId);
            if (CollectionUtils.isEmpty(executionNodeSummaryList)) {
                return;
            }
            for (ExecutionNodeSummary executionNodeSummary : executionNodeSummaryList) {
                final NodeExecuteStatusEnum executeStatus = executionNodeSummary.getExecuteStatus();
                // 仅处理全部任务是成功态的，否则不自动结单
                if (executeStatus.isSuccessExecute()) {
                    continue;
                }
                return;
            }


            String msg = String.format("flowId %s, will auto close, flow status switch to %s。", flowId, FlowStatusEnum.TERMINATE);
            log.warn(msg);

            UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
            updateExecutionFlowDTO.setFlowId(flowId);
            updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.TERMINATE);
            executionFlowAopEventService.finishFlowAop(flowEntity, flowStatus);

            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            executionFlowService.updateFlow(updateExecutionFlowDTO);

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            BeanUtils.copyProperties(updateExecutionFlowDTO, updateBmrFlowDto);
            updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.TERMINATED);
            updateBmrFlowDto.setFlowStatus(BmrFlowStatus.SUCCESS);
            bmrFlowService.alterFlowStatus(updateBmrFlowDto);

            log.warn("flowId {} auto close success...", flowId);
        } catch (Exception e) {
            log.info("tryTerminateFlow error, flow id is {}", flowId);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 检查并更新工作流的审批状态
     *
     * @param underApprovalFlowList
     */
    private void checkAndUpdateFlowApprovalStatus(List<ExecutionFlowEntity> underApprovalFlowList) {
        if (CollectionUtils.isEmpty(underApprovalFlowList)) {
            return;
        }
        for (ExecutionFlowEntity executionFlowEntity : underApprovalFlowList) {
            checkAndUpdateFlowApprovalStatus(executionFlowEntity);
        }
    }

    private void checkAndUpdateFlowApprovalStatus(ExecutionFlowEntity executionFlowEntity) {
        try {
            Long flowId = executionFlowEntity.getId();
            String orderId = executionFlowEntity.getOrderId();
            String operator = executionFlowEntity.getOperator();

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            updateBmrFlowDto.setFlowId(flowId);
            //    UNDER_APPROVAL, // 审批中 or 待我处理
            //    APPROVED,       // 已完结（审批通过）
            //    DISCARDED,      // 已废弃
            //    CANCEL_DEPLOY,      //      取消发布

            if (StringUtils.isBlank(orderId)) {
                // throw new IllegalArgumentException(String.format("flow status is under approval, but can not find order id, flow id is %s", flowId));
                executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.APPROVAL_NOT_PASS);
                executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, "required orderId is blank，flow status switch to 'APPROVAL_NOT_PASS(审批未通过)'");
                updateBmrFlowDto.setApplyState("DISCARDED");
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                executionFlowAopEventService.giveUpFlowAop(executionFlowEntity);
                return;
            }
            OAForm oaForm = oaService.queryForm(operator, orderId);
            OAFormStatus oafFormStatus = oaForm == null ? OAFormStatus.NOT_ACCESSABLE : oaForm.getStatus();

            switch (oafFormStatus) {
//                完结->审批通过
                case APPROVED:
                    executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.APPROVAL_PASS);
                    executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, "flow status switch to 'APPROVAL_PASS(审批通过)'");
                    updateBmrFlowDto.setApplyState("APPROVED");
                    updateBmrFlowDto.setFlowStatus(BmrFlowStatus.READY);
                    bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                    break;
//                废弃->审批未通过
                case DISCARDED:
                    executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.APPROVAL_NOT_PASS);
                    executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, "flow status switch to 'APPROVAL_NOT_PASS(审批未通过)'");
                    updateBmrFlowDto.setApplyState("DISCARDED");
                    bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                    executionFlowAopEventService.giveUpFlowAop(executionFlowEntity);
                    break;
//                无法访问->审批失败
                case NOT_ACCESSABLE:
                    executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.APPROVAL_FAIL);
                    executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, "flow status switch to 'APPROVAL_FAIL(审批失败)'");
                    updateBmrFlowDto.setApplyState("DISCARDED");
                    bmrFlowService.alterFlowStatus(updateBmrFlowDto);
                    executionFlowAopEventService.giveUpFlowAop(executionFlowEntity);
                    break;
                case UNDER_APPROVAL:
                    break;
                default:
                    throw new RuntimeException("un handle oa form status " + oafFormStatus);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            String message = String.format("【%s】工作流id=%s,查询审批状态失败，\n 异常原因：%s \n 详情：%s",
                    active, executionFlowEntity.getId(), e.getMessage(), executionFlowService.generateFlowUrl(executionFlowEntity));
            wxPublisherService.wxPushMsg(executionFlowService.getOpAdminList(), Constants.MSG_TYPE_TEXT, message);
        }
    }

    @Override
    public void updateFlowStatus(Long flowId) {
        ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(flowId);
        if (executionFlowEntity == null) {
            return;
        }
        FlowStatusEnum flowStatus = executionFlowEntity.getFlowStatus();
        List<FlowStatusEnum> handleFlowStatus = getRequireHandleFlowStatus();

        if (!handleFlowStatus.contains(flowStatus)) {
            log.warn("flow status is {}, will ignore, flowId:{}", flowStatus, flowId);
            return;
        }
        updateOneFlowStatus(executionFlowEntity);
    }

    @Override
    @Scheduled(cron = "0/5 * * * * ?")
    public void updateNodeExecuteStatus() {
        boolean isLock = false;
        String lockKey = RedisLockKey.GLOBAL_UPDATE_SCHEDULER_EXECUTE_NODE_STATUS_LOCK_KEY.name() + "-" + SpringApplicationContext.getEnv();
        try {
            isLock = redissonLockSupport.tryLock(lockKey, 3, -1, TimeUnit.SECONDS);
            if (!isLock) {
                log.warn("ignore update work flow node status, active:{}", active);
                return;
            }
            log.info("start update node execute status");
            List<NodeExecuteStatusEnum> handleNodeExecuteStatus = getRequireHandleNodeExecuteStatus();
            List<ExecutionNodeEntity> executionNodeEntities = executionNodeService.findExecuteNodeByNodeStatus(handleNodeExecuteStatus);
            updateNodeExecuteStatus(executionNodeEntities);

            Thread.sleep(3_000);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (isLock) {
                redissonLockSupport.unLock(lockKey);
            }
        }
    }

    @Override
    public void updateNodeExecuteStatus(Long executionNodeId) {
        ExecutionNodeEntity executionNodeEntity = executionNodeService.getById(executionNodeId);
        if (executionNodeEntity == null) {
            return;
        }
        NodeExecuteStatusEnum nodeStatus = executionNodeEntity.getNodeStatus();
        List<NodeExecuteStatusEnum> handleNodeExecuteStatus = getRequireHandleNodeExecuteStatus();

        if (!handleNodeExecuteStatus.contains(nodeStatus)) {
            log.warn("node status is {}, will ignore, executionNodeId:{}", nodeStatus, executionNodeId);
            return;
        }
        updateOneNodeExecuteStatus(executionNodeEntity);
    }

    private void updateFlowStatus(List<ExecutionFlowEntity> executionFlowEntities) {
        if (CollectionUtils.isEmpty(executionFlowEntities)) {
            return;
        }
        for (ExecutionFlowEntity executionFlowEntity : executionFlowEntities) {
            try {
                updateOneFlowStatus(executionFlowEntity);
            } catch (Exception e) {
                try {
                    log.error("loop update flow status error, case by: " + e.getMessage());
                    log.error(e.getMessage(), e);
                    String message = String.format("[%s][告警] 更新flow状态出现异常，id:%s, 发布类型:%, 异常原因:%s \n %s",
                            active, executionFlowEntity.getId(), executionFlowEntity.getDeployType(), e.getMessage(),
                            executionFlowService.generateFlowUrl(executionFlowEntity));
                    wxPublisherService.wxPushMsg(executionFlowService.getOpAdminList(), Constants.MSG_TYPE_TEXT, message);
                } catch (Exception exception) {
                    log.error(exception.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void updateOneFlowStatus(ExecutionFlowEntity executionFlowEntity) {
        FlowStatusEnum flowStatus = executionFlowEntity.getFlowStatus();
        if (FlowStatusEnum.IN_ROLLBACK.equals(flowStatus)) {
            rollbackBusFactoryService.getRollbackFactory(executionFlowEntity)
                    .handlerUpdateFlowStatus(executionFlowEntity);
            // handlerInRollbackFlow(executionFlowEntity);
        } else {
            handlerInExecuteFlow(executionFlowEntity);
        }
    }

    private void handlerInExecuteFlow(ExecutionFlowEntity executionFlowEntity) {
        Long flowId = executionFlowEntity.getId();

        // refresh from db
        executionFlowEntity = executionFlowService.getById(flowId);
        final FlowRollbackType flowRollbackType = executionFlowEntity.getFlowRollbackType();
        // 仅处理正向执行
        if (flowRollbackType != FlowRollbackType.NONE) {
            return;
        }

        Integer currentBatchId = executionFlowEntity.getCurBatchId();

        // 处理待回滚节点
        final ExecutionNodeEntity queryDO = new ExecutionNodeEntity();
        queryDO.setFlowId(flowId);
        queryDO.setExecType(NodeExecType.WAITING_FORWARD);
        queryDO.setNodeType(null);
        final List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeList(queryDO, true);
        if (!CollectionUtils.isEmpty(nodeEntityList)) {
            String warnMsg = String.format("require handle of 'WAITING_FORWARD' node list size is %s", nodeEntityList.size());
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
            Map<NodeExecuteStatusEnum, List<ExecutionNodeEntity>> statusNodesMap = new HashMap<>();
            Map<NodeExecuteStatusEnum, List<Long>> statusNodeIdsMap = new HashMap<>();

            for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
                final NodeExecuteStatusEnum nodeStatus = nodeEntity.getNodeStatus();
                statusNodesMap.computeIfAbsent(nodeStatus, K -> new ArrayList<>()).add(nodeEntity);
                statusNodeIdsMap.computeIfAbsent(nodeStatus, K -> new ArrayList<>()).add(nodeEntity.getId());
            }

            for (Map.Entry<NodeExecuteStatusEnum, List<Long>> entry : statusNodeIdsMap.entrySet()) {
                final NodeExecuteStatusEnum nodeStatus = entry.getKey();
                final List<Long> nodeIds = entry.getValue();
                // 边缘状态未执行任务切换至跳过状态
                boolean matched = false;
                switch (nodeStatus) {
                    case ROLLBACK_SKIPPED:
                        executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.FORWARD, NodeExecuteStatusEnum.SKIPPED);
                        matched = true;
                        break;
                    case UN_NODE_ROLLBACK_EXECUTE:
                    case RECOVERY_UN_NODE_ROLLBACK_EXECUTE:
                    case ROLLBACK_SKIPPED_WHEN_UN_NODE_EXECUTE:
                        executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.FORWARD, NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                        matched = true;
                        break;
                }
                if (!matched && !nodeStatus.isInRollback()) {
                    executionNodeService.batchUpdateNodeForReadyExec(flowId, nodeIds, NodeExecType.FORWARD, NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    continue;
                }
            }
        }

        // 容错度
        Integer tolerance = executionFlowEntity.getTolerance();
        Integer maxBatchId = executionFlowEntity.getMaxBatchId();

        List<NodeExecuteStatusEnum> nodeStatus = new ArrayList<>();
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);
        List<ExecutionNodeEntity> executionUnNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
        if (!CollectionUtils.isEmpty(executionUnNodeEntities)) {
            // 存在待执行的, flow状态不做任何变更
            return;
        }

        nodeStatus.clear();
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE);
        List<ExecutionNodeEntity> executionRunningNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);
        if (!CollectionUtils.isEmpty(executionRunningNodeEntities)) {
            // 当前批次有任务处于执行中, 更新flow状态为执行中
            // executionFlowService.updateFlowStatusByFlowId(flowId, FlowStatusEnum.IN_EXECUTE);

            String logMsg = String.format("maxBatchId: %s,current batch id: %s has node status is IN_EXECUTE", maxBatchId, currentBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);
            return;
        }
        // 清空
        nodeStatus.clear();
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE);
        List<ExecutionNodeEntity> executionFailNodeEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, currentBatchId, nodeStatus);

        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setFlowId(flowId);
        updateExecutionFlowDTO.setRollbackType(FlowRollbackType.NONE);

        if (!CollectionUtils.isEmpty(executionFailNodeEntities)) {
            // 当前批次失败节点数
            int failCount = executionFailNodeEntities.size();
            int totalFailCount = executionFlowEntity.getCurFault() + failCount;
            // 更新当前flow失败节点个数
            executionFlowService.updateCurFault(flowId, totalFailCount);

            String logMsg = String.format("update flow fail count, current batch failCount: %s, total failCount: %s",
                    failCount, totalFailCount);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

            if (totalFailCount != 0 && totalFailCount > tolerance) {
                // 失败节点个数大于容错度, 更新flow状态为执行失败
                updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.FAIL_EXECUTE);
                updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
                executionFlowService.updateFlow(updateExecutionFlowDTO);
                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                updateBmrFlowDto.setFlowId(flowId);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.FAILED_PAUSE);
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);

                String warnMsg = String.format("update flow status to FAIL_EXECUTE, because failed node count Greater than fault-tolerant failCount: %s, tolerance: %s", totalFailCount, tolerance);
                executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, warnMsg);
                return;
            }
        }

        Integer nextBatchId = currentBatchId + 1;
        if (nextBatchId <= maxBatchId) {
            executionFlowService.updateCurrentBatchIdByFlowId(flowId, nextBatchId);
            String msg = String.format("flow maxBatchId Greater than nextBatchId, will continue execute next batch,nextBatchId: %s, maxBatchId: %s", nextBatchId, maxBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            return;
        }

        //  当前批次和最大批次相同, 且错误的任务个数小于容错度, 更改flow状态为成功
        if (currentBatchId.equals(maxBatchId)) {
            log.info("current batch id equal max batch id, this the last batch , will update flow status success");

            updateExecutionFlowDTO.setEndTime(LocalDateTime.now());
            updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.SUCCEED_EXECUTE);

            String msg = String.format("current batch id equal max batch id, this the last batch, flowId: %s, maxBatchId: %s, currentBatchId: %s, flow all node has execute completed, update flow status to success, ", flowId, maxBatchId, currentBatchId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, msg);
            executionFlowService.updateFlow(updateExecutionFlowDTO);

            UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
            BeanUtils.copyProperties(updateExecutionFlowDTO, updateBmrFlowDto);
            updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.DONE_BUT_NOT_FINISH);
            bmrFlowService.alterFlowStatus(updateBmrFlowDto);
        }
    }

    private void updateNodeExecuteStatus(List<ExecutionNodeEntity> executionNodeEntities) {
        for (ExecutionNodeEntity executionNodeEntity : executionNodeEntities) {
            try {
                updateOneNodeExecuteStatus(executionNodeEntity);
            } catch (Exception e) {
                try {
                    log.error("loop update node status error, case by: " + e.getMessage());
                    log.error(e.getMessage(), e);
                    String message = String.format("[%s][告警] 更新node状态出现异常，工作流id:%s,节点名称:%s, 异常原因:%s",
                            active, executionNodeEntity.getFlowId(), executionNodeEntity.getNodeName(), e.getMessage());
                    wxPublisherService.wxPushMsg(executionFlowService.getOpAdminList(), Constants.MSG_TYPE_TEXT, message);
                } catch (Exception exception) {
                    log.error(exception.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void updateOneNodeExecuteStatus(ExecutionNodeEntity executionNodeEntity) {
        NodeExecuteStatusEnum nodeStatus = executionNodeEntity.getNodeStatus();

        switch (nodeStatus) {
            case SKIPPED:
            case FAIL_SKIP_NODE_EXECUTE:
            case FAIL_SKIP_NODE_RETRY_EXECUTE:
            case FAIL_SKIP_NODE_ROLLBACK_EXECUTE:
            case ROLLBACK_SKIPPED:
                return;
        }

        Long id = executionNodeEntity.getId();
        Long flowId = executionNodeEntity.getFlowId();
        ExecutionFlowEntity executionFlowEntity = executionFlowService.getById(flowId);
//        FlowDeployType deployType = executionFlowEntity.getDeployType();
//        Integer curBatchId = executionFlowEntity.getCurBatchId();
//        if (!curBatchId.equals(executionNodeEntity.getBatchId()) && !(nodeStatus.isInRetry() || nodeStatus.isInRollback())) {
//            log.info("skip event, cur batch id is {}, node status is {}", curBatchId, nodeStatus);
//            return;
//        }

        Long executionNodeId = executionNodeEntity.getId();
        List<ExecutionNodeEventEntity> executionNodeEventEntities = executionNodeEventService.queryByExecutionNodeIdAndFlowId(flowId, executionNodeId);
        List<EventStatusEnum> eventStatusEnums = executionNodeEventEntities.stream().map(executionNodeEvent -> executionNodeEvent.getEventStatus()).collect(Collectors.toList());

        if (eventStatusEnums.contains(EventStatusEnum.FAIL_EVENT_EXECUTE)) {
            NodeExecuteStatusEnum nodeNodeExecuteStatus = NodeExecuteStatusEnum.getNodeNodeExecuteStatus(EventStatusEnum.FAIL_EVENT_EXECUTE, nodeStatus);
            log.info("event error before node status is {}, after is {}, flow id is {}, host name is {}", nodeStatus, nodeNodeExecuteStatus, flowId, executionNodeEntity.getNodeName());
            executionNodeService.updateNodeStatusById(id, nodeNodeExecuteStatus);
            updateResourceStatus(executionNodeEntity, nodeNodeExecuteStatus);
            wxFailNotify(executionNodeEntity, executionFlowEntity);
            return;
        }

        if (eventStatusEnums.contains(EventStatusEnum.IN_EVENT_EXECUTE)) {
            NodeExecuteStatusEnum nodeNodeExecuteStatus = NodeExecuteStatusEnum.getNodeNodeExecuteStatus(EventStatusEnum.IN_EVENT_EXECUTE, nodeStatus);
            executionNodeService.updateNodeStatusById(id, nodeNodeExecuteStatus);
            updateResourceStatus(executionNodeEntity, nodeNodeExecuteStatus);
            return;
        }

        if (eventStatusEnums.contains(EventStatusEnum.SKIPPED)) {
            NodeExecuteStatusEnum nodeNodeExecuteStatus = NodeExecuteStatusEnum.getNodeNodeExecuteStatus(EventStatusEnum.SKIPPED, nodeStatus);
            executionNodeService.updateNodeStatusById(id, nodeNodeExecuteStatus);
            updateResourceStatus(executionNodeEntity, nodeNodeExecuteStatus);
            return;
        }

//            if (eventStatusEnums.contains(EventStatusEnum.UN_EVENT_EXECUTE)) {
//                NodeExecuteStatusEnum nodeNodeExecuteStatus = getNodeNodeExecuteStatus(EventStatusEnum.UN_EVENT_EXECUTE, executionNodeEntity.getNodeStatus());
//                executionNodeService.updateNodeStatusById(id, nodeNodeExecuteStatus);
//                continue;
//            }

        if (!eventStatusEnums.contains(EventStatusEnum.FAIL_EVENT_EXECUTE) && !eventStatusEnums.contains(EventStatusEnum.IN_EVENT_EXECUTE)
                && !eventStatusEnums.contains(EventStatusEnum.UN_EVENT_EXECUTE)) {
            // processInstanceExecCacheManager.removeByProcessInstanceId(flowId);
            NodeExecuteStatusEnum nodeNodeExecuteStatus = NodeExecuteStatusEnum.getNodeNodeExecuteStatus(EventStatusEnum.SUCCEED_EVENT_EXECUTE, nodeStatus);
            executionNodeService.updateNodeStatusById(id, nodeNodeExecuteStatus);
            updateResourceStatus(executionNodeEntity, nodeNodeExecuteStatus);
        }
    }

    /**
     * 微信失败通知
     *
     * @param executionNodeEntity
     */
    private void wxFailNotify(ExecutionNodeEntity executionNodeEntity, ExecutionFlowEntity executionFlowEntity) {
//        String errorMessage = String.format("bmr组件发布执行的工作流id为%s下的%s任务执行失败，环境为%s"
//                , executionNodeEntity.getFlowId(), executionNodeEntity.getNodeName(), active);
        String errorMsg = String.format("【%s告警】BMR变更节点｜任务执行出现异常,请关注：", active);
        final StringBuilder builder = new StringBuilder();

        builder.append(errorMsg).append(Constants.NEW_LINE)
                .append(String.format("变更类型：%s", executionFlowEntity.getDeployType().getDesc())).append(Constants.NEW_LINE)
                .append("组件：" + executionFlowEntity.getComponentName()).append(Constants.NEW_LINE)
                .append("集群：" + executionFlowEntity.getClusterName()).append(Constants.NEW_LINE)
                .append("执行节点：" + executionNodeEntity.getNodeName()).append(Constants.NEW_LINE)
                .append("详情：" + executionFlowService.generateFlowUrl(executionFlowEntity));

        executionFlowAopEventService.jobFailAop(executionFlowEntity, executionNodeEntity, builder.toString());
    }

    /**
     * 更新资源管理系统的状态
     *
     * @param executionNodeEntity
     * @param nodeNodeExecuteStatus
     */
    private void updateResourceStatus(ExecutionNodeEntity executionNodeEntity, NodeExecuteStatusEnum nodeNodeExecuteStatus) {
//       执行中的节点跳过
        if (!(nodeNodeExecuteStatus.isSuccessExecute() || nodeNodeExecuteStatus.failExecute())) {
            return;
        }
        ExecutionFlowEntity executionFlow = executionFlowService.getById(executionNodeEntity.getFlowId());
        executionNodeEntity = executionNodeService.getById(executionNodeEntity.getId());
        try {
            executionFlowAopEventService.jobFinishAop(executionFlow, executionNodeEntity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<FlowStatusEnum> getRequireHandleFlowStatus() {
        List<FlowStatusEnum> flowStatusEnums = new ArrayList<>();
        flowStatusEnums.add(FlowStatusEnum.UN_EXECUTE);
        flowStatusEnums.add(FlowStatusEnum.IN_EXECUTE);
        flowStatusEnums.add(FlowStatusEnum.IN_ROLLBACK);
        return flowStatusEnums;
    }

    private List<NodeExecuteStatusEnum> getRequireHandleNodeExecuteStatus() {
        List<NodeExecuteStatusEnum> nodeStatus = new ArrayList<>();
//        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
//        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);
//        nodeStatus.add(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE);
        nodeStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE);

        return nodeStatus;
    }
}
