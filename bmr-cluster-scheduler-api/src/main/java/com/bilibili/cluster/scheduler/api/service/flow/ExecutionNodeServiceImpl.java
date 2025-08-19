package com.bilibili.cluster.scheduler.api.service.flow;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.configuration.MasterConfig;
import com.bilibili.cluster.scheduler.api.exceptions.WorkflowInstanceTaskEventHandleException;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceTaskEvent;
import com.bilibili.cluster.scheduler.api.scheduler.handler.WorkflowInstanceTaskEventHandler;
import com.bilibili.cluster.scheduler.api.service.redis.RedisService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.SaberUpdateProp;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsDeleteJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsUpdateJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.node.BaseExecutionNodeDTO;
import com.bilibili.cluster.scheduler.common.dto.node.BatchNodeExecDTO;
import com.bilibili.cluster.scheduler.common.dto.node.RichedExecutionNodeDTO;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRetryNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchRollBackNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.BatchSkipNodeReq;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryNodePageReq;
import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkPeripheryComponentDeployJobExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodePropsEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.dolphin.TaskPosType;
import com.bilibili.cluster.scheduler.common.enums.event.EventStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import com.bilibili.cluster.scheduler.common.utils.CacheUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.NetUtils;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionNodeEventMapper;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionNodeMapper;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */

@Slf4j
@Service
public class ExecutionNodeServiceImpl extends ServiceImpl<ExecutionNodeMapper, ExecutionNodeEntity> implements ExecutionNodeService {

    @Resource
    private ExecutionNodeMapper executionNodeMapper;

    @Resource
    private ExecutionNodeEventMapper executionNodeEventMapper;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    private ExecutionNodePropsService executionNodePropsService;


    @Resource
    private WorkflowInstanceTaskEventHandler workflowInstanceTaskEventHandler;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    ExecutionLogService executionLogService;

    @Resource
    private MasterConfig masterConfig;

    @Resource
    private ExecutionNodePropsService nodePropsService;

    @Resource
    RedisService redisService;

    @Override
    public List<ExecutionNodeEntity> queryExecutionNodeByBatchIdAndFlowId(Long flowId, Integer batchId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getBatchId, batchId);
        return list(queryWrapper);
    }

    @Override
    public List<ExecutionNodeEntity> queryByFlowIdAndBatchIdAndNodeStatus(Long flowId, Integer batchId, List<NodeExecuteStatusEnum> jobStatus) {
        return executionNodeMapper.queryByFlowIdAndNodeStatus(flowId, batchId, jobStatus);
    }

    @Override
    public List<ExecutionNodeEntity> findExecuteNodeByNodeStatus(List<NodeExecuteStatusEnum> nodeStatus) {
        return executionNodeMapper.findExecuteNodeByNodeStatus(nodeStatus);
    }

    @Override
    public void updateNodeStatusById(Long id, NodeExecuteStatusEnum nodeExecuteStatusEnum) {
        executionNodeMapper.updateNodeStatusById(id, nodeExecuteStatusEnum);
    }

    @Override
    public void updateNodeStatusByFlowIdAndNodeStatus(Long flowId, NodeExecuteStatusEnum originJobExecuteStatus, NodeExecuteStatusEnum targetJobExecuteStatus) {
        executionNodeMapper.updateNodeStatusByFlowIdAndNodeStatus(flowId, originJobExecuteStatus, targetJobExecuteStatus);
    }

    @Override
    public void batchUpdateNodeStatus(List<Long> jobIdList, NodeExecuteStatusEnum nodeExecuteStatusEnum) {
        if (CollectionUtils.isEmpty(jobIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.in(ExecutionNodeEntity::getId, jobIdList)
                .set(ExecutionNodeEntity::getNodeStatus, nodeExecuteStatusEnum);
        update(updateWrapper);
    }

    @Override
    public void updateNodeStatusByFlowIdAndBatchId(Long flowId, Integer batchId, NodeExecuteStatusEnum nodeExecuteStatusEnum) {
        executionNodeMapper.updateNodeStatusByFlowIdAndBatchId(flowId, batchId, nodeExecuteStatusEnum);
    }

    @Override
    public boolean batchInsert(List<ExecutionNodeEntity> executionNodeEntityList) {
        return baseMapper.batchInsert(executionNodeEntityList);
    }

    @Override
    public ResponseResult queryNodePage(QueryNodePageReq req) {
        IPage result = queryFlowNodeList(req);

        final List<BaseExecutionNodeDTO> records = result.getRecords();
        Map<Long, BaseExecutionNodeDTO> executionJobIdToExecutionJobDTOMap = records.stream().collect(Collectors.toMap(ExecutionNodeEntity::getId, Function.identity()));
        Set<Long> jobIdSet = executionJobIdToExecutionJobDTOMap.keySet();
        if (CollectionUtils.isEmpty(jobIdSet)) {
            return ResponseResult.getSuccess(result);
        }
        List<ExecutionNodeEventEntity> executionNodeEventEntityList = executionNodeEventService.queryEventListByNodeIds(jobIdSet);
        for (ExecutionNodeEventEntity executionNodeEventEntity : executionNodeEventEntityList) {
            Long jobId = executionNodeEventEntity.getExecutionNodeId();
            BaseExecutionNodeDTO saberExecutionJobDTO = executionJobIdToExecutionJobDTOMap.get(jobId);
            saberExecutionJobDTO.getExecutionNodeEventEntityList().add(executionNodeEventEntity);
        }

        final ExecutionFlowEntity flowEntity = executionFlowService.getById(req.getFlowId());
        final FlowDeployType deployType = flowEntity.getDeployType();
        switch (deployType) {
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
                final List<RichedExecutionNodeDTO<SparkDeployJobExtParams>> sparkRichedExecutionNodeDTOList = generateRichedExecutionNodeList(records, jobIdSet, SparkDeployJobExtParams.class);
                result.setRecords(sparkRichedExecutionNodeDTOList);
                break;
            case HBO_JOB_PARAM_RULE_UPDATE:
                final List<RichedExecutionNodeDTO<HboJobParamsUpdateJobExtParams>> hboUpdateRichedExecutionNodeDTOList = generateRichedExecutionNodeList(records, jobIdSet, HboJobParamsUpdateJobExtParams.class);
                result.setRecords(hboUpdateRichedExecutionNodeDTOList);
                break;
            case HBO_JOB_PARAM_RULE_DELETE:
                final List<RichedExecutionNodeDTO<HboJobParamsDeleteJobExtParams>> hboDeleteRichedExecutionNodeDTOList = generateRichedExecutionNodeList(records, jobIdSet, HboJobParamsDeleteJobExtParams.class);
                result.setRecords(hboDeleteRichedExecutionNodeDTOList);
                break;
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                final List<RichedExecutionNodeDTO<SparkPeripheryComponentDeployJobExtParams>> sparkPeripheryNodeDTOList = generateRichedExecutionNodeList(records, jobIdSet, SparkPeripheryComponentDeployJobExtParams.class);
                result.setRecords(sparkPeripheryNodeDTOList);
                break;
            case SPARK_EXPERIMENT:
                final List<RichedExecutionNodeDTO<ExperimentJobProps>> sparkExperimentJobDTOList = generateRichedExecutionNodeList(records, jobIdSet, ExperimentJobProps.class);
                result.setRecords(sparkExperimentJobDTOList);
                break;
            default:
                break;
        }
        return ResponseResult.getSuccess(result);
    }

    private <T extends BaseNodeParams> List<RichedExecutionNodeDTO<T>> generateRichedExecutionNodeList(List<BaseExecutionNodeDTO> records, Set<Long> jobIdSet, Class<T> clazz) {
//        List<T> jobExtParamsList = nodePropsService.queryNodePropsByNodeIdList(jobIdSet, clazz);
        final List<T> jobExtParamsList = jobIdSet.stream().map(nodeId -> {
            T t = nodePropsService.queryNodePropsByNodeId(nodeId, clazz);
            if (!Objects.isNull(t) && !NumberUtils.isPositiveLong(t.getNodeId())) {
                t.setNodeId(nodeId);
            }
            return t;
        }).collect(Collectors.toList());

        final Map<Long, T> deployJobExtParamsMap = jobExtParamsList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        T::getNodeId,
                        Function.identity()
                ));
        final List<RichedExecutionNodeDTO<T>> richedExecutionNodeDTOList = records.stream()
                .map(nodeDTO -> {
                    final RichedExecutionNodeDTO<T> richedNodeDTO = new RichedExecutionNodeDTO<>();
                    BeanUtils.copyProperties(nodeDTO, richedNodeDTO);
                    final Long nodeId = nodeDTO.getId();
                    final T jobExtParams = deployJobExtParamsMap.get(nodeId);
                    richedNodeDTO.setNodeProps(jobExtParams);
                    return richedNodeDTO;
                }).collect(Collectors.toList());
        return richedExecutionNodeDTOList;
    }

    private IPage<BaseExecutionNodeDTO> queryFlowNodeList(QueryNodePageReq req) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, req.getFlowId())
                .like(!StringUtils.isBlank(req.getNodeName()), ExecutionNodeEntity::getNodeName, req.getNodeName())
                .eq(!Objects.isNull(req.getNodeStatus()), ExecutionNodeEntity::getNodeStatus, req.getNodeStatus())
                .eq(!StringUtils.isBlank(req.getExecStage()), ExecutionNodeEntity::getExecStage, req.getExecStage())
                .eq(!StringUtils.isBlank(req.getNodeType()), ExecutionNodeEntity::getNodeType, req.getNodeType())
                .orderByAsc(ExecutionNodeEntity::getId);
        if (req.isSkipLogicalNode()) {
            queryWrapper.in(ExecutionNodeEntity::getNodeType, NodeType.getNormalExecNodeList());
        }
        Page<ExecutionNodeEntity> page = new Page<>(req.getPageNum(), req.getPageSize());
        IPage<BaseExecutionNodeDTO> result = baseMapper.queryNodePage(page, queryWrapper);
        return result;
    }

    @Override
    public List<ExecutionNodeEntity> queryAllExecutionNodeByFlowId(Long flowId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId);
        queryWrapper.orderByAsc(ExecutionNodeEntity::getId);
        return list(queryWrapper);
    }

    @Override
    public void updateNodeStartTimeOrEndTime(Long id, long instanceId, LocalDateTime startTime, LocalDateTime endTime) {
        executionNodeMapper.updateNodeStartTimeOrEndTime(id, instanceId, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult batchRetryNode(BatchRetryNodeReq req) {
        try {
            Long flowId = req.getFlowId();
            ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);
            if (!flowEntity.getFlowStatus().isRunning()) {
                return ResponseResult.getError("工作流已结单或处于待执行状态");
            }

            List<Long> nodeIdList = req.getNodeIdList();
            List<String> nodeNameList = req.getNodeNameList();
            if (CollectionUtils.isEmpty(nodeIdList) && CollectionUtils.isEmpty(nodeNameList)) {
                return ResponseResult.getError("选择的节点列表为空");
            }

            FlowDeployType deployType = flowEntity.getDeployType();
            String componentName = flowEntity.getComponentName();
            List<ExecutionNodeEntity> executionNodeEntityList;
//            ck的容器发布重试需要重试所有的节点
            if (deployType.isContainer() && Constants.CLICK_HOUSE_COMPONENT.equalsIgnoreCase(componentName)) {
                executionNodeEntityList = queryExecutionNodeByFlowId(flowId);
            } else {
                // 优先使用nodeId做过滤
                if (!CollectionUtils.isEmpty(nodeIdList)) {
                    executionNodeEntityList = getExecutionNodeByNodeIdList(nodeIdList, flowId);
                } else {
                    executionNodeEntityList = getExecutionNodeByNodeNameList(nodeNameList, flowId);
                }
            }

            if (CollectionUtils.isEmpty(executionNodeEntityList)) {
                return ResponseResult.getError("未查询到任何待重试的节点列表");
            }

//            List<ExecutionNodeEntity> waitRetryIdList = new ArrayList<>();
//            List<ExecutionNodeEntity> waitRollBackList = new ArrayList<>();
//
//            List<Long> jobIds = new ArrayList<>();
//            for (ExecutionNodeEntity executionJob : executionNodeEntityList) {
//                jobIds.add(executionJob.getId());
//                NodeExecuteStatusEnum jobStatus = executionJob.getNodeStatus();
//                if (jobStatus.failRollBack()) {
//                    // 回滚失败->待回滚
//                    waitRollBackList.add(executionJob);
//                    continue;
//                }
//                // 变更失败，重试失败，跳过->待重试
//                if (jobStatus.failExecute()) {
//                    waitRetryIdList.add(executionJob);
//                }
//            }
//
//            batchUpdateNodeStatusAndResetInstanceId(
//                    waitRollBackList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList()),
//                    NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
//            batchUpdateNodeStatusAndResetInstanceId(
//                    waitRetryIdList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList()),
//                    NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);
//
//            List<ExecutionNodeEntity> targetExecutionNodeList = new ArrayList<>();
//            targetExecutionNodeList.addAll(waitRetryIdList);
//            targetExecutionNodeList.addAll(waitRollBackList);
//            List<Long> targetJobIdList = targetExecutionNodeList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
//            // 更新事件状态为未执行状态
////            executionNodeEventService.batchUpdateEventStatusWithoutSuccess(targetJobIdList, EventStatusEnum.UN_EVENT_EXECUTE);
//            executionNodeEventService.batchUpdateEventStatusAndResetInstanceId(targetJobIdList,
//                    EventStatusEnum.UN_EVENT_EXECUTE, true);
//            // 更新操作状态为正常
//            batchUpdateNodeOperationResult(targetJobIdList, NodeOperationResult.NORMAL);

            List<ExecutionNodeEntity> targetExecutionNodeList = batchUpdateNodeAndEventForRetry(executionNodeEntityList, true);

            String logMsg = String.format("%s will retry execute flow, flowId: %s, ", MDC.get(Constants.REQUEST_USER), flowId);
            logMsg += "retry node list is:\n " + targetExecutionNodeList.stream().map(ExecutionNodeEntity::getNodeName).collect(Collectors.toList());
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);
            BatchNodeExecDTO batchNodeExecDTO = new BatchNodeExecDTO(flowEntity, targetExecutionNodeList);
            return ResponseResult.getSuccess(batchNodeExecDTO);
        } catch (Exception e) {
            log.error(String.format("batch retry job error, req is: %s, error is %s", JSONUtil.toJsonStr(req), e.getMessage()));
            return ResponseResult.getError("批量重试失败");
        }
    }

    private List<ExecutionNodeEntity> getExecutionNodeByNodeIdList(List<Long> nodeIdList, Long flowId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(ExecutionNodeEntity::getId, nodeIdList);
        List<ExecutionNodeEntity> executionJobEntityList = list(queryWrapper);
        return executionJobEntityList;
    }


    /**
     * 本机处理job
     *
     * @param executionJobEntityList
     * @throws WorkflowInstanceTaskEventHandleException
     */
    public void handlerJobTaskEvent(List<ExecutionNodeEntity> executionJobEntityList, ExecutionFlowEntity executionFlowEntity) throws WorkflowInstanceTaskEventHandleException {
        if (CollectionUtils.isEmpty(executionJobEntityList)) {
            log.warn("executionJobEntityList size is null, will not execute handlerJobTaskEvent....");
            return;
        }

        Long flowId = executionFlowEntity.getId();
        String hostName = NetUtils.getAddr(masterConfig.getListenPort());
        ExecutionFlowInstanceDTO executionFlowInstanceDTO = executionFlowService.generateExecutionFlowInstance(hostName, executionFlowEntity, null);

        WorkflowInstanceTaskEvent workflowInstanceTaskEvent = new WorkflowInstanceTaskEvent(executionFlowInstanceDTO.getInstanceId(), null, flowId, hostName, executionFlowInstanceDTO);
        log.info("work flow instance is {}", JSONUtil.toJsonStr(workflowInstanceTaskEvent));
        workflowInstanceTaskEventHandler.handleWorkflowJobTaskEvent(workflowInstanceTaskEvent, executionJobEntityList);
    }

    @Override
    public Integer queryMaxBatchId(Long flowId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>()
                .select("max(`batch_id`) as `maxBatchId`").lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId);
        Map<String, Object> resultMap = getMap(queryWrapper);
        if (CollectionUtils.isEmpty(resultMap)) {
            return -1;
        }
        Integer maxBatchId = Integer.parseInt(resultMap.get("maxBatchId").toString());
        return maxBatchId;
    }


    @Override
    public long queryFlowFailureNodesCount(long flowId) {
        final LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(ExecutionNodeEntity::getNodeStatus,
                        Arrays.asList(NodeExecuteStatusEnum.FAIL_NODE_EXECUTE, NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE))
                .eq(ExecutionNodeEntity::getDeleted, false);
        return count(queryWrapper);
    }


    @Override
    public void clearPodByPodName(Long flowId, List<String> podNameList) {
        if (CollectionUtils.isEmpty(podNameList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(ExecutionNodeEntity::getPodName, podNameList)
                .set(ExecutionNodeEntity::getPodName, Constants.EMPTY_STRING)
                .set(ExecutionNodeEntity::getPodStatus, Constants.EMPTY_STRING);
        update(updateWrapper);
    }

    @Override
    public void updatePodName(Long nodeId, String podName) {
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>()
                .lambda();
        updateWrapper.eq(ExecutionNodeEntity::getId, nodeId)
                .set(ExecutionNodeEntity::getPodName, podName);
        update(updateWrapper);
    }

    @Override
    public void updatePodStatus(Long nodeId, String podStatus) {
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEntity::getId, nodeId)
                .set(ExecutionNodeEntity::getPodStatus, podStatus);
        update(updateWrapper);
    }

    private List<ExecutionNodeEntity> getExecutionNodeByNodeNameList(List<String> nodeNameList, Long flowId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(ExecutionNodeEntity::getNodeName, nodeNameList);
        List<ExecutionNodeEntity> executionJobEntityList = list(queryWrapper);
        return executionJobEntityList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult batchRollBackNode(BatchRollBackNodeReq req) {
        try {
            Long flowId = req.getFlowId();
            ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);
            if (!flowEntity.getFlowStatus().isRunning()) {
                return ResponseResult.getError("工作流已结单或处于待执行状态");
            }

            List<ExecutionNodeEntity> executionJobList = getExecutionNodeByNodeNameList(req.getNodeNameList(), flowId);
            Iterator<ExecutionNodeEntity> iterator = executionJobList.iterator();
            while (iterator.hasNext()) {
                ExecutionNodeEntity executionJobEntity = iterator.next();
                if (!executionJobEntity.getNodeStatus().canRollBack()) {
                    iterator.remove();
                }
            }
            List<Long> jobIds = executionJobList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
            executionNodeEventService.batchUpdateEventStatusAndResetInstanceId(jobIds,
                    EventStatusEnum.UN_EVENT_EXECUTE, true);
            // 更新事件状态为未执行状态
            batchUpdateNodeStatusAndResetInstanceId(
                    jobIds,
                    NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
            // 更新操作状态为正常
            batchUpdateNodeOperationResult(jobIds, NodeOperationResult.NORMAL);

            String logMsg = String.format("will roll back execute flow, flowId:%s", flowId);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

            BatchNodeExecDTO batchNodeExecDTO = new BatchNodeExecDTO(flowEntity, executionJobList);
            return ResponseResult.getSuccess(batchNodeExecDTO);
        } catch (Exception e) {
            log.error(String.format("batch rollback job error, job id list is %s, error is %s", req.getNodeNameList(), e.getMessage()));
            return ResponseResult.getError("回滚失败");
        }
    }

    @Override
    public void recoveryFlowNode(ExecutionFlowEntity executionFlowEntity) {
        try {
            Long flowId = executionFlowEntity.getId();
            Integer curBatchId = executionFlowEntity.getCurBatchId();
            List<NodeExecuteStatusEnum> jobStatus = new ArrayList<>();
            jobStatus.add(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE);

            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE);

            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_RETRY_EXECUTE);
            jobStatus.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);

            List<ExecutionNodeEntity> recoveryFlowJobs = executionNodeMapper.getByFlowIdAndNodeStatus(flowId, jobStatus);
            if (CollectionUtils.isEmpty(recoveryFlowJobs)) {
                log.info("flowId {}, not find any nodes require recovery, skipped...");
                return;
            }
            for (ExecutionNodeEntity executionJobEntity : recoveryFlowJobs) {
                Long id = executionJobEntity.getId();
                executionNodeEventMapper.updateEventStatusWithoutSuccessByExecutionNodeId(id, EventStatusEnum.UN_EVENT_EXECUTE);
                executionNodeMapper.updateNodeStatusById(id, NodeExecuteStatusEnum.getRecoveryJobStatus(executionJobEntity.getNodeStatus()));
            }
            log.info("flowId {}, start recovery node list is {}", flowId, JSONUtil.toJsonStr(recoveryFlowJobs));
            handlerJobTaskEvent(recoveryFlowJobs, executionFlowEntity);
        } catch (Exception e) {
            log.error("recovery flow job has error, flowId: " + executionFlowEntity.getId(), e);
        }
    }

    @Override
    public void batchUpdateNodeOperationResult(List<Long> jobIdList, NodeOperationResult operationResult) {
        if (CollectionUtils.isEmpty(jobIdList)) {
            log.warn("job id list is null, will not execute update job operation result....");
            return;
        }

        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.in(ExecutionNodeEntity::getId, jobIdList)
                .set(ExecutionNodeEntity::getOperationResult, operationResult);
        update(updateWrapper);
    }

    @Override
    public void updateNodeOperationResultByNodeId(Long jobId, NodeOperationResult operationResult, long instanceId) {
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEntity::getId, jobId)
                .eq(ExecutionNodeEntity::getInstanceId, instanceId)
                .set(ExecutionNodeEntity::getOperationResult, operationResult);
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult batchSkipNode(BatchSkipNodeReq req) {
        Long flowId = req.getFlowId();
        ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);
        if (!flowEntity.getFlowStatus().isRunning()) {
            return ResponseResult.getError("工作流已结单或处于待执行状态");
        }

        List<ExecutionNodeEntity> executionJobList = getExecutionNodeByNodeNameList(req.getNodeNameList(), flowId);
        Iterator<ExecutionNodeEntity> iterator = executionJobList.iterator();
        while (iterator.hasNext()) {
            ExecutionNodeEntity executionJobEntity = iterator.next();
            if (!executionJobEntity.getNodeStatus().canSkip()) {
                iterator.remove();
            }
        }
        List<Long> executionJobIdList = executionJobList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
        batchUpdateNodeStatus(executionJobIdList, NodeExecuteStatusEnum.SKIPPED);
        batchUpdateNodeOperationResult(executionJobIdList, NodeOperationResult.USER_SKIP);


        String logMsg = String.format("user skip job list, operator is %s, job id list is %s", MDC.get(Constants.REQUEST_USER), executionJobIdList);
        executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);

        return ResponseResult.getSuccess();

    }

    @Override
    public void queryNodePageAndDownLoad(QueryNodePageReq req, HttpServletResponse response) {
        req.setPageNum(Constants.DEFAULT_PAGE_NUM);
        req.setPageSize(Constants.PAGE_MAX);
        IPage<BaseExecutionNodeDTO> executionJobPage = queryFlowNodeList(req);
        List<BaseExecutionNodeDTO> executionJobDTOList = executionJobPage.getRecords();

        try {
            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.write(executionJobDTOList, true);
            //设置content—type
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;");
            response.setCharacterEncoding("utf-8");
            //设置标题
            String fileName = LocalDateFormatterUtils.format(Constants.FMT_DATE_TIME_SECOND, LocalDateTime.now()) + ".xlsx";
            //Content-disposition是MIME协议的扩展，MIME协议指示MIME用户代理如何显示附加的文件。
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ServletOutputStream outputStream = response.getOutputStream();
            //将Writer刷新到OutPut
            writer.flush(outputStream, true);
            outputStream.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ExecutionNodeEntity> queryExecutionNodeByFlowId(Long flowId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId);
        return list(queryWrapper);
    }

    @Override
    public void updateNodeInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList, String execHost) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            log.warn("node id list is null, will not execute update node current instanceId.");
            return;
        }

        log.info("update node current instanceId, flowId: {}, instanceId: {}, nodeIdList: {}", flowId, instanceId, nodeIdList);
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(ExecutionNodeEntity::getId, nodeIdList)
                .set(ExecutionNodeEntity::getInstanceId, instanceId)
                .set(ExecutionNodeEntity::getExecHost, execHost);
        update(updateWrapper);
    }

    @Override
    public List<ExecutionNodeEntity> queryNodeListByInstanceId(long flowId, long instanceId) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getInstanceId, instanceId);
        return list(queryWrapper);
    }

    @Override
    public List<ExecutionNodeEntity> queryByHostname(Long flowId, String nodeName) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getNodeName, nodeName);
        return list(queryWrapper);
    }

    @Override
    public ExecutionNodeEntity queryByHostnameAndInstanceId(Long flowId, String nodeName, Long instanceId) {
        final ExecutionNodeEntity queryDo = new ExecutionNodeEntity();
        queryDo.setFlowId(flowId);
        queryDo.setNodeName(nodeName);
        queryDo.setInstanceId(instanceId);
        queryDo.setNodeType(null);

        return queryOneNode(queryDo);
    }

    @Override
    public void batchUpdateNodeStatusAndResetInstanceId(List<Long> jobIdList, NodeExecuteStatusEnum unNodeRetryExecute) {
        if (CollectionUtils.isEmpty(jobIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.in(ExecutionNodeEntity::getId, jobIdList)
                .set(ExecutionNodeEntity::getNodeStatus, unNodeRetryExecute)
                .set(ExecutionNodeEntity::getInstanceId, 0l);
        update(updateWrapper);
    }

    @Override
    public void updateNodeStatusByIdAndInstanceId(Long executionNodeId, long instanceId, NodeExecuteStatusEnum failNodeExecute) {
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.eq(ExecutionNodeEntity::getId, executionNodeId)
                .eq(ExecutionNodeEntity::getInstanceId, instanceId)
                .set(ExecutionNodeEntity::getNodeStatus, failNodeExecute);
        update(updateWrapper);
    }

    private SaberUpdateProp getPropByPropId(Long propId) {
        if (propId > 0) {
            try {
                ExecutionNodePropsEntity beforePropEntity = executionNodePropsService.getById(propId);
                String beforePropsJson = beforePropEntity.getPropsContent();
                SaberUpdateProp prop = JSONUtil.toBean(beforePropsJson, SaberUpdateProp.class);
                return prop;
            } catch (Exception e) {
                log.error("convert before props error, prop id is " + propId + ", error is " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    @Override
    public List<ExecutionNodeEntity> getAlignNodeListByEventStatus(Long flowId, Long instanceId, int executeOrder, List<EventStatusEnum> eventStatusList) {
        if (executeOrder == 0) {
            return queryNodeListByInstanceId(flowId, instanceId);
        }
        List<Long> nodeIdList = getAlignNodeIdListByEventStatus(flowId, instanceId, executeOrder, eventStatusList);
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda();
        queryWrapper.eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getInstanceId, instanceId)
                .in(ExecutionNodeEntity::getId, nodeIdList);
        return list(queryWrapper);
    }

    @Override
    public List<Long> getAlignNodeIdListByEventStatus(Long flowId, Long instanceId, int executeOrder, List<EventStatusEnum> eventStatusList) {
        if (executeOrder == 0) {
            List<ExecutionNodeEntity> nodeEntityList = queryNodeListByInstanceId(flowId, instanceId);
            if (CollectionUtils.isEmpty(nodeEntityList)) {
                return Collections.emptyList();
            }
            return nodeEntityList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
        }
        List<ExecutionNodeEventEntity> nodeEventList = executionNodeEventService.queryNodeEventListByStatusList(
                flowId, instanceId, executeOrder, eventStatusList);
        if (CollectionUtils.isEmpty(nodeEventList)) {
            return Collections.emptyList();
        }
        return nodeEventList.stream().map(ExecutionNodeEventEntity::getExecutionNodeId).collect(Collectors.toList());
    }

    @Override
    public ExecutionNodeEntity queryOneNode(ExecutionNodeEntity queryDo) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda()
                .eq(NumberUtils.isPositiveLong(queryDo.getId()), ExecutionNodeEntity::getId, queryDo.getId())
                .eq(!StringUtils.isBlank(queryDo.getNodeName()), ExecutionNodeEntity::getNodeName, queryDo.getNodeName())
                .eq(NumberUtils.isPositiveLong(queryDo.getFlowId()), ExecutionNodeEntity::getFlowId, queryDo.getFlowId())
                .eq(NumberUtils.isPositiveInteger(queryDo.getBatchId()), ExecutionNodeEntity::getBatchId, queryDo.getBatchId())
                .eq(NumberUtils.isPositiveLong(queryDo.getExtraPropsId()), ExecutionNodeEntity::getExtraPropsId, queryDo.getExtraPropsId())
                .eq(!StringUtils.isBlank(queryDo.getOperator()), ExecutionNodeEntity::getOperator, queryDo.getOperator())
                .eq(Objects.nonNull(queryDo.getNodeStatus()), ExecutionNodeEntity::getNodeStatus, queryDo.getNodeStatus())
                .eq(Objects.nonNull(queryDo.getCtime()), ExecutionNodeEntity::getCtime, queryDo.getCtime())
                .eq(Objects.nonNull(queryDo.getMtime()), ExecutionNodeEntity::getMtime, queryDo.getMtime())
                .eq(Objects.nonNull(queryDo.getStartTime()), ExecutionNodeEntity::getStartTime, queryDo.getStartTime())
                .eq(Objects.nonNull(queryDo.getEndTime()), ExecutionNodeEntity::getEndTime, queryDo.getEndTime())
                .eq(Objects.nonNull(queryDo.getOperationResult()), ExecutionNodeEntity::getOperationResult, queryDo.getOperationResult())
                .eq(!StringUtils.isBlank(queryDo.getRack()), ExecutionNodeEntity::getRack, queryDo.getRack())
                .eq(!StringUtils.isBlank(queryDo.getIp()), ExecutionNodeEntity::getIp, queryDo.getIp())
                .eq(!StringUtils.isBlank(queryDo.getExecStage()), ExecutionNodeEntity::getExecStage, queryDo.getExecStage())
                .eq(NumberUtils.isPositiveLong(queryDo.getInstanceId()), ExecutionNodeEntity::getInstanceId, queryDo.getInstanceId())
                .eq(Objects.nonNull(queryDo.getNodeType()), ExecutionNodeEntity::getNodeType, queryDo.getNodeType())
                .eq(Objects.nonNull(queryDo.getExecType()), ExecutionNodeEntity::getExecType, queryDo.getExecType())
                .eq(ExecutionNodeEntity::getDeleted, false)
                .last(Constants.LIMIT_ONE);
        return getOne(queryWrapper);
    }

    @Override
    public List<ExecutionNodeEntity> queryNodeList(ExecutionNodeEntity queryDo, boolean isAsc) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>().lambda()
                .eq(NumberUtils.isPositiveLong(queryDo.getId()), ExecutionNodeEntity::getId, queryDo.getId())
                .eq(!StringUtils.isBlank(queryDo.getNodeName()), ExecutionNodeEntity::getNodeName, queryDo.getNodeName())
                .eq(NumberUtils.isPositiveLong(queryDo.getFlowId()), ExecutionNodeEntity::getFlowId, queryDo.getFlowId())
                .eq(NumberUtils.isPositiveInteger(queryDo.getBatchId()), ExecutionNodeEntity::getBatchId, queryDo.getBatchId())
                .eq(NumberUtils.isPositiveLong(queryDo.getExtraPropsId()), ExecutionNodeEntity::getExtraPropsId, queryDo.getExtraPropsId())
                .eq(!StringUtils.isBlank(queryDo.getOperator()), ExecutionNodeEntity::getOperator, queryDo.getOperator())
                .eq(Objects.nonNull(queryDo.getNodeStatus()), ExecutionNodeEntity::getNodeStatus, queryDo.getNodeStatus())
                .eq(Objects.nonNull(queryDo.getCtime()), ExecutionNodeEntity::getCtime, queryDo.getCtime())
                .eq(Objects.nonNull(queryDo.getMtime()), ExecutionNodeEntity::getMtime, queryDo.getMtime())
                .eq(Objects.nonNull(queryDo.getStartTime()), ExecutionNodeEntity::getStartTime, queryDo.getStartTime())
                .eq(Objects.nonNull(queryDo.getEndTime()), ExecutionNodeEntity::getEndTime, queryDo.getEndTime())
                .eq(Objects.nonNull(queryDo.getOperationResult()), ExecutionNodeEntity::getOperationResult, queryDo.getOperationResult())
                .eq(!StringUtils.isBlank(queryDo.getRack()), ExecutionNodeEntity::getRack, queryDo.getRack())
                .eq(!StringUtils.isBlank(queryDo.getIp()), ExecutionNodeEntity::getIp, queryDo.getIp())
                .eq(!StringUtils.isBlank(queryDo.getExecStage()), ExecutionNodeEntity::getExecStage, queryDo.getExecStage())
                .eq(NumberUtils.isPositiveLong(queryDo.getInstanceId()), ExecutionNodeEntity::getInstanceId, queryDo.getInstanceId())
                .eq(Objects.nonNull(queryDo.getNodeType()), ExecutionNodeEntity::getNodeType, queryDo.getNodeType())
                .eq(Objects.nonNull(queryDo.getExecType()), ExecutionNodeEntity::getExecType, queryDo.getExecType())
                .eq(ExecutionNodeEntity::getDeleted, false)
                .orderBy(true, isAsc, ExecutionNodeEntity::getId);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeAndEventInstanceId(Long flowId, Long instanceId, List<Long> nodeIdList, String execHost) {
        updateNodeInstanceId(flowId, instanceId, nodeIdList, execHost);
        executionNodeEventService.updateBatchNodeEventInstanceId(flowId,
                instanceId, nodeIdList);
    }


    @Override
    public List<ExecutionNodeSummary> queryExecutionNodeSummary(Long flowId) {
        return executionNodeMapper.selectExecutionNodeSummary(flowId);
    }

    @Override
    public String queryCurStage(long flowId, Integer batchId) {
        final ExecutionNodeEntity queryDO = new ExecutionNodeEntity();
        queryDO.setFlowId(flowId);
        queryDO.setBatchId(batchId);
        queryDO.setNodeType(null);
        final ExecutionNodeEntity nodeEntity = queryOneNode(queryDO);
        if (Objects.isNull(nodeEntity)) {
            return Constants.EMPTY_STRING;
        }
        return nodeEntity.getExecStage();
    }

    @Override
    public ExecutionNodeEntity queryCurExecOneNode(long flowId, Integer batchId) {
        final ExecutionNodeEntity queryDO = new ExecutionNodeEntity();
        queryDO.setFlowId(flowId);
        queryDO.setBatchId(batchId);
        queryDO.setNodeType(null);
        final ExecutionNodeEntity nodeEntity = queryOneNode(queryDO);
        return nodeEntity;
    }

    @Override
    public void batchUpdateNodeStatusAndInstanceId(long flowId, List<Long> nodeIdList, NodeExecuteStatusEnum nextNodeStatus,
                                                   Long instanceId, String execHost) {
        if (CollectionUtils.isEmpty(nodeIdList)) {
            return;
        }
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.in(ExecutionNodeEntity::getId, nodeIdList)
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .set(ExecutionNodeEntity::getNodeStatus, nextNodeStatus)
                .set(ExecutionNodeEntity::getInstanceId, instanceId)
                .set(ExecutionNodeEntity::getExecHost, execHost);
        update(updateWrapper);
    }

    @Override
    public Long queryMaxNodeIdByStage(Long flowId, String execStage) {
        String cacheKey = CacheUtils.getMaxNodeIdByStageCacheKey(SpringApplicationContext.getEnv(), flowId, execStage);
        final String valueObj = redisService.get(cacheKey);
        if (!StringUtils.isBlank(valueObj)) {
            return Long.parseLong(valueObj);
        }
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>()
                .select("max(id) as maxNodeId").lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getExecStage, execStage)
                .eq(ExecutionNodeEntity::getDeleted, 0);

        Map<String, Object> resultMap = getMap(queryWrapper);
        if (CollectionUtils.isEmpty(resultMap)) {
            return -1l;
        }
        Long maxNodeId = Long.parseLong(resultMap.get("maxNodeId").toString());
        redisService.set(cacheKey, String.valueOf(maxNodeId), Constants.ONE_MINUTES * 5);
        return maxNodeId;
    }

    @Override
    public String queryMaxStageByFlowId(Long flowId) {
        // 使用逻辑节点，不再需要缓存最大stage信息
//        String cacheKey = CacheUtils.getFlowMaxStageCacheKey(SpringApplicationContext.getEnv(), flowId);
//        final String valueObj = redisService.get(cacheKey);
//        if (!StringUtils.isBlank(valueObj)) {
//            return valueObj;
//        }
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>()
                .select("max(exec_stage) as maxStage").lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0);
        Map<String, Object> resultMap = getMap(queryWrapper);
        if (CollectionUtils.isEmpty(resultMap)) {
            return Constants.EMPTY_STRING;
        }
        String stage = resultMap.getOrDefault("maxStage", Constants.EMPTY_STRING).toString();
//        redisService.set(cacheKey, stage, Constants.ONE_MINUTES * 5);
        return stage;
    }


    @Override
    public String queryMinStageByFlowId(Long flowId) {
        String cacheKey = CacheUtils.getFlowMinStageCacheKey(SpringApplicationContext.getEnv(), flowId);
        final String valueObj = redisService.get(cacheKey);
        if (!StringUtils.isBlank(valueObj)) {
            return valueObj;
        }
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>()
                .select("min(exec_stage) as minStage").lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0);
        Map<String, Object> resultMap = getMap(queryWrapper);
        if (CollectionUtils.isEmpty(resultMap)) {
            return Constants.EMPTY_STRING;
        }
        String stage = resultMap.getOrDefault("minStage", Constants.EMPTY_STRING).toString();
        redisService.set(cacheKey, stage, Constants.ONE_MINUTES * 5);
        return stage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateNodeForReadyExec(Long flowId, List<Long> nodeIds, NodeExecType nodeExecType, NodeExecuteStatusEnum nodeExecuteStatus) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return false;
        }
        LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda();
        updateWrapper.in(ExecutionNodeEntity::getId, nodeIds)
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .set(ExecutionNodeEntity::getNodeStatus, nodeExecuteStatus)
                .set(ExecutionNodeEntity::getExecType, nodeExecType)
                .set(ExecutionNodeEntity::getInstanceId, 0l);
        update(updateWrapper);
        executionNodeEventService.batchUpdateEventStatusAndResetInstanceId(nodeIds, EventStatusEnum.UN_EVENT_EXECUTE, true);
        return true;
    }

    @Override
    public int queryMinBatchIdByStage(Long flowId, String curStage) {
        LambdaQueryWrapper<ExecutionNodeEntity> queryWrapper = new QueryWrapper<ExecutionNodeEntity>()
                .select("min(batch_id) as minBatchId").lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getExecStage, curStage)
                .eq(ExecutionNodeEntity::getDeleted, 0);
        Map<String, Object> resultMap = getMap(queryWrapper);
        if (CollectionUtils.isEmpty(resultMap)) {
            return 1;
        }
        String minBatchId = resultMap.getOrDefault("minBatchId", "1").toString();
        return Integer.parseInt(minBatchId);

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult batchRetryFailedEvent(BatchRetryNodeReq req, boolean skipFailedEvent) {
        try {
            Long flowId = req.getFlowId();
            ExecutionFlowEntity flowEntity = executionFlowService.getById(flowId);
            if (!flowEntity.getFlowStatus().isRunning()) {
                return ResponseResult.getError("工作流当前未处于运行状态");
            }

            List<Long> nodeIdList = req.getNodeIdList();
            List<String> nodeNameList = req.getNodeNameList();
            if (CollectionUtils.isEmpty(nodeIdList) && CollectionUtils.isEmpty(nodeNameList)) {
                return ResponseResult.getError("选择的节点列表为空");
            }

            List<ExecutionNodeEntity> executionNodeEntityList;
            // 优先使用nodeId做过滤
            if (!CollectionUtils.isEmpty(nodeIdList)) {
                executionNodeEntityList = getExecutionNodeByNodeIdList(nodeIdList, flowId);
            } else {
                executionNodeEntityList = getExecutionNodeByNodeNameList(nodeNameList, flowId);
            }

            if (CollectionUtils.isEmpty(executionNodeEntityList)) {
                return ResponseResult.getError("未查询到任何待重试的节点列表");
            }

            List<ExecutionNodeEventEntity> failedEventList = new ArrayList<>();
            for (ExecutionNodeEntity nodeEntity : executionNodeEntityList) {
                if (!nodeEntity.getNodeStatus().isFail()) {
                    return ResponseResult.getError("节点未处于失败状态：" + nodeEntity.getNodeName());
                }
                final Long nodeId = nodeEntity.getId();
                List<ExecutionNodeEventEntity> nodeEventList = executionNodeEventService.queryNodeEventListByNodeId(nodeId);
                Preconditions.checkState(!CollectionUtils.isEmpty(nodeEventList), "nodeEventList is blank, node name is :" + nodeEntity.getNodeName());
                boolean hasErrorEvent = false;
                for (ExecutionNodeEventEntity eventEntity : nodeEventList) {
                    if (eventEntity.getEventStatus().isFailed()) {
                        final EventTypeEnum eventType = eventEntity.getEventType();
                        if (!eventType.isSupportRetry()) {
                            return ResponseResult.getError(nodeEntity.getNodeName() +
                                    "，该节点失败事件不支持错误重试：" + eventType.getDesc());
                        }
                        if (eventType.isDolphinType()) {
                            TaskPosType taskPosType = TaskPosType.valueOf(eventEntity.getTaskPosType());
                            if (!taskPosType.isStartNode()) {
                                return ResponseResult.getError(nodeEntity.getNodeName() +
                                        "，该节点失败事件是dolphin类型，非开始节点，不支持错误重试，事件名称：" + eventEntity.getEventName());
                            }
                        }

                        failedEventList.add(eventEntity);
                        // 仅添加首个出错事件
                        hasErrorEvent = true;
                        break;
                    }
                }
                if (!hasErrorEvent) {
                    return ResponseResult.getError("未找到出错事件：" + nodeEntity.getNodeName());
                }
            }

            // 所有的错误节点和出错事件已经找到
            if (skipFailedEvent) {
                // 把出错事件置为跳过状态
                List<Long> eventIdList = failedEventList.stream().map(ExecutionNodeEventEntity::getId).collect(Collectors.toList());
                executionNodeEventService.updateBatchEventStatusByIdList(flowId, eventIdList, EventStatusEnum.EVENT_SKIPPED);
            }

            List<ExecutionNodeEntity> targetExecutionNodeList = batchUpdateNodeAndEventForRetry(executionNodeEntityList, false);

            String logMsg = skipFailedEvent ?
                    String.format("%s will skip failed events and retry execute, flowId: %s, ", MDC.get(Constants.REQUEST_USER), flowId) :
                    String.format("%s will retry execute failed events, flowId: %s, ", MDC.get(Constants.REQUEST_USER), flowId);
            logMsg += "reExecute node list is:\n " + targetExecutionNodeList.stream().map(ExecutionNodeEntity::getNodeName).collect(Collectors.toList());
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, logMsg);
            BatchNodeExecDTO batchNodeExecDTO = new BatchNodeExecDTO(flowEntity, targetExecutionNodeList);
            return ResponseResult.getSuccess(batchNodeExecDTO);
        } catch (Exception e) {
            log.error(String.format("batch retry job error, req is: %s, error is %s", JSONUtil.toJsonStr(req), e.getMessage()));
            return ResponseResult.getError("批量重试失败");
        }
    }

    private List<ExecutionNodeEntity> batchUpdateNodeAndEventForRetry(List<ExecutionNodeEntity> executionNodeEntityList, boolean reExecute) {
        List<ExecutionNodeEntity> waitRetryIdList = new ArrayList<>();
        List<ExecutionNodeEntity> waitRollBackList = new ArrayList<>();

        List<Long> jobIds = new ArrayList<>();
        for (ExecutionNodeEntity executionJob : executionNodeEntityList) {
            jobIds.add(executionJob.getId());
            NodeExecuteStatusEnum jobStatus = executionJob.getNodeStatus();
            if (jobStatus.failRollBack()) {
                // 回滚失败->待回滚
                waitRollBackList.add(executionJob);
                continue;
            }
            // 变更失败，重试失败，跳过->待重试
            if (jobStatus.failExecute()) {
                waitRetryIdList.add(executionJob);
            }
        }

        batchUpdateNodeStatusAndResetInstanceId(
                waitRollBackList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList()),
                NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
        batchUpdateNodeStatusAndResetInstanceId(
                waitRetryIdList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList()),
                NodeExecuteStatusEnum.UN_NODE_RETRY_EXECUTE);

        List<ExecutionNodeEntity> targetExecutionNodeList = new ArrayList<>();
        targetExecutionNodeList.addAll(waitRetryIdList);
        targetExecutionNodeList.addAll(waitRollBackList);
        List<Long> targetJobIdList = targetExecutionNodeList.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
        // 更新事件状态为未执行状态
//            executionNodeEventService.batchUpdateEventStatusWithoutSuccess(targetJobIdList, EventStatusEnum.UN_EVENT_EXECUTE);
        executionNodeEventService.batchUpdateEventStatusAndResetInstanceId(targetJobIdList,
                EventStatusEnum.UN_EVENT_EXECUTE, reExecute);
        // 更新操作状态为正常
        batchUpdateNodeOperationResult(targetJobIdList, NodeOperationResult.NORMAL);
        return targetExecutionNodeList;
    }


    @Override
    public int queryCurrentFlowRunningNodeSize(Long flowId) {
        List<NodeExecuteStatusEnum> runningStateList = new ArrayList<>();
        runningStateList.add(NodeExecuteStatusEnum.IN_NODE_EXECUTE);
        runningStateList.add(NodeExecuteStatusEnum.IN_NODE_RETRY_EXECUTE);
        runningStateList.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_EXECUTE);
        runningStateList.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_RETRY_EXECUTE);
        runningStateList.add(NodeExecuteStatusEnum.RECOVERY_UN_NODE_ROLLBACK_EXECUTE);
        runningStateList.add(NodeExecuteStatusEnum.RECOVERY_IN_NODE_ROLLBACK_EXECUTE);

        List<ExecutionNodeEntity> nodeEntityList = queryNodeListByStateList(flowId, runningStateList);
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return 0;
        } else {
            return nodeEntityList.size();
        }
    }

    @Override
    public List<ExecutionNodeEntity> queryNodeListByStateList(Long flowId, List<NodeExecuteStatusEnum> statusList) {
        LambdaQueryWrapper<ExecutionNodeEntity> query = new LambdaQueryWrapper<ExecutionNodeEntity>()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(!CollectionUtil.isEmpty(statusList), ExecutionNodeEntity::getNodeStatus, statusList)
                .eq(ExecutionNodeEntity::getDeleted, false);
        return list(query);
    }

    @Override
    public List<ExecutionNodeEntity> queryNextRequireExecNodesList(Long flowId, Integer curBatchId, List<NodeExecuteStatusEnum> statusList, int requireExecCnt) {
        LambdaQueryWrapper<ExecutionNodeEntity> query = new LambdaQueryWrapper<ExecutionNodeEntity>()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .in(!CollectionUtil.isEmpty(statusList), ExecutionNodeEntity::getNodeStatus, statusList)
                .ge(ExecutionNodeEntity::getBatchId, curBatchId)
                .eq(ExecutionNodeEntity::getDeleted, false)
                .orderByAsc(ExecutionNodeEntity::getId)
                .last("limit " + requireExecCnt);
        return list(query);
    }


}
