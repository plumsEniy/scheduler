package com.bilibili.cluster.scheduler.api.service.flow;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.redis.RedissonLockSupport;
import com.bilibili.cluster.scheduler.api.scheduler.cache.ProcessInstanceExecCacheManager;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.flow.BmrFlowService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.caster.CasterService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.FlowPrepareService;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.FlowRollbackFactory;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.bus.FlowRollbackBusFactoryService;
import com.bilibili.cluster.scheduler.api.service.incident.IncidentTransferService;
import com.bilibili.cluster.scheduler.api.service.oa.OAService;
import com.bilibili.cluster.scheduler.api.tools.FlowDetailAdaptor;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.config.ConfigDetailData;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.button.OpStrategyButton;
import com.bilibili.cluster.scheduler.common.dto.flow.*;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.QueryFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.flow.req.spark.QuerySparkDeployFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.hbo.pararms.HboJobParamsUpdateFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.ComponentConfInfo;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.node.dto.ExecutionNodeSummary;
import com.bilibili.cluster.scheduler.common.dto.node.req.QueryHostExecutionFlowPageReq;
import com.bilibili.cluster.scheduler.common.dto.oa.OAForm;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.CodeDiff;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.OaChangeInfo;
import com.bilibili.cluster.scheduler.common.dto.oa.manager.ReplaceRoleModel;
import com.bilibili.cluster.scheduler.common.dto.presto.tide.PrestoTideExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.scheduler.model.SchedTaskDefine;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployHistoryDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployType;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientType;
import com.bilibili.cluster.scheduler.common.dto.spark.params.*;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEventEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.event.EventTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.*;
import com.bilibili.cluster.scheduler.common.enums.flowLog.LogTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeExecType;
import com.bilibili.cluster.scheduler.common.response.ResponseResult;
import com.bilibili.cluster.scheduler.common.utils.EnvUtils;
import com.bilibili.cluster.scheduler.common.utils.LocalDateFormatterUtils;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.bilibili.cluster.scheduler.dao.mapper.ExecutionFlowMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-19
 */
@Service
@Slf4j
public class ExecutionFlowServiceImpl extends ServiceImpl<ExecutionFlowMapper, ExecutionFlowEntity> implements ExecutionFlowService {

    @Resource
    private BmrFlowService bmrFlowService;

    @Resource
    private RedissonLockSupport redissonLockSupport;

    @Resource
    private ExecutionFlowMapper executionFlowMapper;

    @Resource
    private ExecutionNodeService executionNodeService;

    @Resource
    private ExecutionLogService executionLogService;

    @Resource
    private ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    private ProcessInstanceExecCacheManager processInstanceExecCacheManager;

    @Resource
    private ExecutionNodeEventService executionNodeEventService;

    @Resource
    private ExecutionFlowService executionFlowService;

    @Resource
    private CasterService casterService;

    @Resource
    OAService oaService;

    @Resource
    GlobalService globalService;

    @Resource
    private BmrResourceService bmrResourceService;

    @Resource
    private BmrMetadataService bmrMetadataService;

    @Resource
    private ExecutionFlowAopEventService executionFlowAopEventService;

    @Resource
    private BmrConfigService bmrConfigService;

    @Value("${spring.profiles.active}")
    private String active;

    @Value("${oa-flow.approver-list}")
    private String[] approverList;

    @Value("${spark.deploy.dev_Leader}")
    private String[] sparkDevLeaders;

    @Value("${spark.deploy.sre_Leader}")
    private String[] sparkSreLeaders;

    @Value("${hbo.deploy.dev_Leader}")
    private String[] hboDevLeaders;

    @Value("${hbo.deploy.sre_Leader}")
    private String[] hboSreLeaders;

    @Value("${sre.deploy.dev_Leader}")
    private String[] sreDevLeaders;

    @Value("${sre.deploy.sre_Leader}")
    private String[] sreSreLeaders;

    @Value("${hdfs.deploy.approver}")
    private String[] hdfsDeployApprovers;

    @Value("${hdfs.deploy.dev_Leader}")
    private String[] hdfsDevLeaders;

    @Value("${hdfs.deploy.sre_Leader}")
    private String[] hdfsSreLeaders;

    @Value("#{'${bmr.scheduler.admin.list:}'.empty ? null : '${bmr.scheduler.admin.list:}'.split(',')}")
    List<String> opAdminList;

    @Resource
    FlowPrepareService flowPrepareService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    FlowRollbackBusFactoryService rollbackBusFactoryService;

    @Resource
    IncidentTransferService incidentTransferService;

    @Override
    public List<ExecutionFlowEntity> findExecuteFlowPageBySlot(int pageSize, int pageNumber, int masterCount, int thisMasterSlot) {
        if (masterCount <= 0) {
            return Lists.newArrayList();
        }
        List<ExecutionFlowEntity> list = executionFlowMapper.findExecuteFlowPageBySlot(pageSize, pageNumber * pageSize, masterCount, thisMasterSlot);
        return list;
    }

    @Override
    public List<ExecutionFlowEntity> findExecuteFlowByFlowStatus(List<FlowStatusEnum> flowStatusEnums) {
        return executionFlowMapper.findExecuteFlowByFlowStatus(flowStatusEnums);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void updateFlowStatusByFlowId(Long flowId, FlowStatusEnum flowStatusEnum) {
        executionFlowMapper.updateFlowStatusById(flowId, flowStatusEnum);
    }

    @Override
    public void updateFlow(UpdateExecutionFlowDTO updateExecutionFlowDTO) {
        executionFlowMapper.updateFlow(updateExecutionFlowDTO);
    }

    @Override
    public void updateFlowStatusAndCurrentBatchIdByFlowId(Long flowId, FlowStatusEnum flowStatusEnum, Integer currentBatchId) {
        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setFlowId(flowId);
        updateExecutionFlowDTO.setFlowStatus(flowStatusEnum);
        updateExecutionFlowDTO.setCurrentBatchId(currentBatchId);
        executionFlowMapper.updateFlow(updateExecutionFlowDTO);
    }

    @Override
    public ExecutionFlowInstanceDTO handleWorkFlowProcess(String host, ExecutionFlowEntity executionFlowEntity) throws Exception {
        try {
            Long flowId = executionFlowEntity.getId();
            Integer curBatchId = executionFlowEntity.getCurBatchId();
            List<NodeExecuteStatusEnum> jobStatus = new ArrayList<>();

            // 仅调度未执行状态的节点，
            // TODO: 需要强保证调度不出错，否则节点状态流转会出问题
            if (FlowStatusEnum.IN_ROLLBACK.equals(executionFlowEntity.getFlowStatus())) {
                jobStatus.add(NodeExecuteStatusEnum.UN_NODE_ROLLBACK_EXECUTE);
            } else {
                jobStatus.add(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            }
            boolean isSlideWindow = judgeFlowRequireSlideWindowExec(executionFlowEntity);
            List<ExecutionNodeEntity> executionJobEntities;
            if (isSlideWindow) {
                log.info("flowId {} use slide window exec nodes.", flowId);
                final Integer parallelism = executionFlowEntity.getParallelism();
                int runningCnt = executionNodeService.queryCurrentFlowRunningNodeSize(flowId);
                if (runningCnt >= parallelism) {
                    log.info("flowId {} running nodes cnt {} already overhead parallelism {}, skip schedule un-exec nodes.", flowId, runningCnt, parallelism);
                    return null;
                }
                int requireExecCnt = parallelism - runningCnt;
                executionJobEntities = executionNodeService.queryNextRequireExecNodesList(flowId, curBatchId, jobStatus, requireExecCnt);
                if (!CollectionUtils.isEmpty(executionJobEntities)) {
                    String msg = flowId + ", use slide window exec next nodes, size is : " + executionJobEntities.size();
                    executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, msg);
                }
            } else {
                executionJobEntities = executionNodeService.queryByFlowIdAndBatchIdAndNodeStatus(flowId, curBatchId, jobStatus);
            }

            if (CollectionUtils.isEmpty(executionJobEntities)) {
                // 当前批次已经处于执行状态, 进行忽略
                String logMsg = String.format("current batch not contain any un-exec status nodes, will ignore ");
                executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, logMsg);
                return null;
            }

            // generate instance id
            ExecutionFlowInstanceDTO executionFlowInstanceDTO = generateExecutionFlowInstance(host, executionFlowEntity, curBatchId);

            // double-check flow status
            ExecutionFlowEntity executionFlow = executionFlowMapper.selectById(executionFlowEntity.getId());
            FlowStatusEnum flowStatus = executionFlow.getFlowStatus();

            if (!flowStatus.equals(executionFlowEntity.getFlowStatus())) {
                log.warn("flow status already change, skip job dispatcher. flow id is: " + flowId);
                return null;
            }

            // 待执行状态
            if (FlowStatusEnum.UN_EXECUTE.equals(flowStatus)) {
                // 修改flow状态为执行中
                UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
                updateExecutionFlowDTO.setFlowId(executionFlowEntity.getId());
                updateExecutionFlowDTO.setFlowStatus(FlowStatusEnum.IN_EXECUTE);
                updateExecutionFlowDTO.setHostName(host);
                updateExecutionFlowDTO.setStartTime(LocalDateTime.now());

                executionFlowService.updateFlow(updateExecutionFlowDTO);
                UpdateBmrFlowDto updateBmrFlowDto = new UpdateBmrFlowDto();
                BeanUtils.copyProperties(updateExecutionFlowDTO, updateBmrFlowDto);
                updateBmrFlowDto.setOpStrategy(BmrFlowOpStrategy.PROCEED_ALL);
                updateBmrFlowDto.setFlowStatus(BmrFlowStatus.RUNNING);
                bmrFlowService.alterFlowStatus(updateBmrFlowDto);

                String logMsg = String.format("update flow status to IN_EXECUTE, flowId: %s, params: %s", executionFlowEntity.getId(), JSONUtil.toJsonStr(updateExecutionFlowDTO));
                log.info(logMsg);
                executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, logMsg);
            }

            if (!CollectionUtils.isEmpty(executionJobEntities)) {
                // 更改当前批次处于执行中状态 ---> 更改当前批次满足调度条件的节点处于执行中状态
                NodeExecuteStatusEnum nextNodeStatus;
                if (FlowStatusEnum.IN_ROLLBACK.equals(flowStatus)) {
                    nextNodeStatus = NodeExecuteStatusEnum.IN_NODE_ROLLBACK_EXECUTE;
                } else {
                    nextNodeStatus = NodeExecuteStatusEnum.IN_NODE_EXECUTE;
                }
                // executionNodeService.updateNodeStatusByFlowIdAndBatchId(flowId, curBatchId, nextNodeStatus);
                // 确定节点列表
                final List<Long> nodeIdList = executionJobEntities.stream().map(ExecutionNodeEntity::getId).collect(Collectors.toList());
                // 更新节点列表的状态,同时设置节点的InstanceId和执行主机
                // executionNodeService.batchUpdateNodeStatus(nodeIdList, nextNodeStatus);
                executionNodeService.batchUpdateNodeStatusAndInstanceId(flowId, nodeIdList, nextNodeStatus,
                        executionFlowInstanceDTO.getInstanceId(), host);

                // 调度节点的兜底逻辑
                if (!host.equals(executionFlow.getHostName())) {
                    UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
                    updateExecutionFlowDTO.setFlowId(executionFlowEntity.getId());
                    updateExecutionFlowDTO.setHostName(host);
                    executionFlowService.updateFlow(updateExecutionFlowDTO);
                    String logMsg = String.format("update current flow execute on host, before is %s, after is %s.", executionFlow.getHostName(), host);
                    log.info(logMsg);
                    executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, logMsg);
                }

                String logMsg = String.format("update current batch job status %s, curBatchId: %s", nextNodeStatus, curBatchId);
                log.info(logMsg);
                executionLogService.updateLogContent(executionFlowEntity.getId(), LogTypeEnum.FLOW, logMsg);
            }

            return executionFlowInstanceDTO;
        } catch (Exception e) {
            log.error("handleWorkFlowProcess has error, flowId is {}", executionFlowEntity.getId(), e);
            throw new Exception(e);
        }
    }

    private boolean judgeFlowRequireSlideWindowExec(ExecutionFlowEntity flowEntity) {
        final FlowDeployType deployType = flowEntity.getDeployType();

        // 当前spark实验开启滑动窗口执行
        if (deployType.equals(FlowDeployType.SPARK_EXPERIMENT)) {
            return true;
        }
        return false;
    }

    public ExecutionFlowInstanceDTO generateExecutionFlowInstance(String host, ExecutionFlowEntity executionFlowEntity, Integer curBatchId) {
//        todo:先不增加额外参数
        ExecutionFlowProps executionFlowProps = new ExecutionFlowProps();
        BeanUtils.copyProperties(executionFlowEntity, executionFlowProps);

        Long flowId = executionFlowEntity.getId();

        ExecutionFlowInstanceDTO executionFlowInstanceDTO = new ExecutionFlowInstanceDTO();
        executionFlowInstanceDTO.setFlowId(flowId);
        executionFlowInstanceDTO.setDeployType(executionFlowEntity.getDeployType());
        executionFlowInstanceDTO.setHostName(host);
        executionFlowInstanceDTO.setInstanceId(RandomUtil.randomLong(Long.MAX_VALUE));
        executionFlowInstanceDTO.setCurrentBatchId(curBatchId);
        executionFlowInstanceDTO.setMaxBatchId(executionFlowEntity.getMaxBatchId());
        executionFlowInstanceDTO.setOperator(executionFlowEntity.getOperator());
        executionFlowInstanceDTO.setExecutionFlowProps(executionFlowProps);
        executionFlowInstanceDTO.setAutoRetry(executionFlowEntity.getAutoRetry());
        executionFlowInstanceDTO.setMaxRetry(executionFlowEntity.getMaxRetry());
        return executionFlowInstanceDTO;
    }

    @Override
    public ResponseResult queryHostExecutionFlowPage(QueryHostExecutionFlowPageReq req) {
        Page<ExecutionFlowEntity> page = new Page<>(req.getPageNum(), req.getPageSize());
        IPage<ExecutionFlowEntity> executionFlowEntityIPage = executionFlowMapper.queryHostExecutionFlowPage(page, req);
        return ResponseResult.getSuccess(executionFlowEntityIPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult executionOneFlow(Long flowId) {

        try {
            ExecutionFlowEntity executionFlow = getById(flowId);
            if (Objects.isNull(executionFlow)) {
                return ResponseResult.getError("can not find flow, flow id is " + flowId);
            }

            FlowStatusEnum flowStatus = executionFlow.getFlowStatus();
            if (flowStatus != FlowStatusEnum.APPROVAL_PASS && flowStatus != FlowStatusEnum.PREPARE_EXECUTE_FAILED) {
                String orderId = executionFlow.getOrderId();
                return ResponseResult.getError(String.format(
                        "审批单未处于【待执行】，当前工作流状态为【%s】%s", flowStatus.getDesc(),
                        StringUtils.isBlank(orderId) ? "" : String.format("，审批单详情：https://shenpi.bilibili.co/process/%s?hl=done", orderId)));
            }

            log.info("prepare execution flow id is {}", flowId);
            updateFlowStatusByFlowId(flowId, FlowStatusEnum.PREPARE_EXECUTE);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, MDC.get(Constants.REQUEST_USER) +
                    ": submit execute flow now, then status to 'PREPARE_EXECUTE(准备执行), please wait prepare stage finish....'");
            return ResponseResult.getSuccess();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseResult.getError("执行工作流错误,错误原因:" + e.getMessage());
        }
    }

    @Override
    public ResponseResult cancelFlow(Long flowId) {
        ExecutionFlowEntity executionFlow = getById(flowId);
        if (Objects.isNull(executionFlow)) {
            return ResponseResult.getError("can not find flow, flow id is " + flowId);
        }

        FlowStatusEnum flowStatus = executionFlow.getFlowStatus();
        if (!FlowStatusEnum.canCancel(flowStatus)) {
            return ResponseResult.getError(String.format("flow status is %s, can not cancel", flowStatus));
        }

        LambdaUpdateWrapper<ExecutionFlowEntity> updateWrapper = new UpdateWrapper<ExecutionFlowEntity>().lambda();
        updateWrapper.eq(ExecutionFlowEntity::getId, flowId)
                .set(ExecutionFlowEntity::getFlowStatus, FlowStatusEnum.CANCEL);
        update(updateWrapper);
        executionFlowAopEventService.giveUpFlowAop(executionFlow);
        return ResponseResult.getSuccess();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult applyFlow(DeployOneFlowReq req) {

        Long clusterId = req.getClusterId();
        MetadataClusterData clusterDetail = null;
        if (NumberUtils.isPositiveLong(clusterId)) {
            clusterDetail = bmrMetadataService.queryClusterDetail(clusterId);
            if (Objects.isNull(clusterDetail)) {
                return ResponseResult.getError("集群不存在,集群id为" + clusterId);
            }
        }

        Long componentId = req.getComponentId();
        MetadataComponentData componentDetail = null;
        if (NumberUtils.isPositiveLong(componentId)) {
            componentDetail = bmrMetadataService.queryComponentByComponentId(componentId);
            if (Objects.isNull(componentDetail)) {
                return ResponseResult.getError("组件不存在,组件id为" + componentId);
            }
        }

        try {
            String currentUser = req.getUserName();
            if (StringUtils.isBlank(currentUser)) {
                currentUser = MDC.get(Constants.REQUEST_USER);
            }
            ExecutionFlowEntity executionFlowEntity = getNewFlowEntity(req, currentUser, clusterDetail, componentDetail);
            List<ResolvedEvent> resolvedEventList = flowPrepareService.resolvePipelineEventList(req,
                    executionFlowEntity, clusterDetail, componentDetail);
            log.info("apply flow resolve event list is {}", resolvedEventList);

            // set event name list
            List<String> eventNameList = resolvedEventList.stream().map(ResolvedEvent::getEventName).collect(Collectors.toList());
            executionFlowEntity.setEventList(JSONUtil.toJsonStr(eventNameList));
            boolean saveFlowResult = executionFlowService.save(executionFlowEntity);
            Assert.isTrue(saveFlowResult, "保存工作流失败");

            Long flowId = executionFlowEntity.getId();
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW,
                    "flow id: " + flowId + " base params save ok");
            saveFlowExtParams(flowId, req);
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW,
                    "flow id: " + flowId + " ext params save ok");

            executionFlowAopEventService.initFlowAopEvent(executionFlowEntity);
            executionFlowAopEventService.createFlowAop(executionFlowEntity);
            return ResponseResult.getSuccess(executionFlowEntity);
        } catch (Exception e) {
            String errorMsg = String.format("save apply flow has error, req:%s, error is %s", JSONUtil.toJsonStr(req), e.getMessage());
            log.error(errorMsg, e);
            throw new RuntimeException("save apply flow has error", e);
        }
    }

    private void saveFlowExtParams(Long flowId, DeployOneFlowReq req) {
        String extParams = req.getExtParams();
        log.info("flowId of {} extParams is {}.", flowId, extParams);

        List<String> nodeList = req.getNodeList();
        if (CollectionUtils.isEmpty(nodeList)) {
            nodeList = Collections.emptyList();
        }

        BaseFlowExtPropDTO baseFlowExtPropDTO = new BaseFlowExtPropDTO();
        baseFlowExtPropDTO.setNodeList(nodeList);
        baseFlowExtPropDTO.setFlowExtParams(extParams);

        executionFlowPropsService.saveFlowProp(flowId, baseFlowExtPropDTO);
    }

    public void updateMaxBatchId(Long flowId, Integer maxBatchId) {
        executionFlowMapper.updateFlowMaxBatchId(flowId, maxBatchId);
    }

    @Override
    @SuppressWarnings("ignore")
    public ResponseResult queryPipelineDefine(Long flowId) throws Exception {
        ExecutionNodeEntity queryNodeDo = new ExecutionNodeEntity();
        queryNodeDo.setFlowId(flowId);
        ExecutionNodeEntity nodeEntity = executionNodeService.queryOneNode(queryNodeDo);

        if (Objects.isNull(nodeEntity)) {
            return resolvePipelineDefine(getById(flowId));
        }

        ExecutionNodeEventEntity queryEventDo = new ExecutionNodeEventEntity();
        queryEventDo.setFlowId(flowId).setExecutionNodeId(nodeEntity.getId());
        List<ExecutionNodeEventEntity> eventEntityList = executionNodeEventService.queryNodeEventList(queryEventDo);

        if (CollectionUtils.isEmpty(eventEntityList)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }

        Map<String, Map<String, SchedTaskDefine>> cacheMap = new HashMap<>();
        List<SchedTaskDefine> taskDefineList = new ArrayList<>();
        int index = 1;
        for (ExecutionNodeEventEntity eventEntity : eventEntityList) {
            SchedTaskDefine taskDefine = new SchedTaskDefine();
            taskDefine.setTaskCode(String.valueOf(index++));
            taskDefine.setName(eventEntity.getEventName());
            taskDefineList.add(taskDefine);
            if (eventEntity.getEventType().isDolphinType()) {
                String projectCode = eventEntity.getProjectCode();
                String pipelineCode = eventEntity.getPipelineCode();
                String cacheKey = projectCode + "_" + pipelineCode;
                String taskCode = eventEntity.getTaskCode();
                SchedTaskDefine define = cacheMap.computeIfAbsent(cacheKey, key -> {
                    List<SchedTaskDefine> schedTaskDefineList = globalService.getDolphinSchedulerInteractService()
                            .parsePipelineDefineByCode(projectCode, pipelineCode);
                    if (CollectionUtils.isEmpty(schedTaskDefineList)) {
                        return Collections.EMPTY_MAP;
                    }
                    return schedTaskDefineList.stream().collect(Collectors.toMap(
                            SchedTaskDefine::getTaskCode, Function.identity(), (o, n) -> n));
                }).get(taskCode);
                if (!Objects.isNull(define)) {
                    taskDefine.setRawScript(define.getRawScript());
                    taskDefine.setPreTaskCode(define.getPreTaskCode());
                }
            } else {
                taskDefine.setRawScript(eventEntity.getEventType().getSummary());
            }
        }
        return ResponseResult.getSuccess(taskDefineList);
    }

    private ResponseResult resolvePipelineDefine(ExecutionFlowEntity flowEntity) throws Exception {
        if (Objects.isNull(flowEntity)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }
        List<ResolvedEvent> resolvedEventList = flowPrepareService.resolvePipelineEventList(null, flowEntity, null, null);
        if (CollectionUtils.isEmpty(resolvedEventList)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }
        Map<String, Map<String, SchedTaskDefine>> cacheMap = new HashMap<>();
        List<SchedTaskDefine> taskDefineList = new ArrayList<>();
        int index = 1;
        for (ResolvedEvent resolvedEvent : resolvedEventList) {
            SchedTaskDefine taskDefine = new SchedTaskDefine();
            taskDefine.setTaskCode(String.valueOf(index++));
            taskDefine.setName(resolvedEvent.getEventName());
            taskDefineList.add(taskDefine);
            final EventTypeEnum eventType = resolvedEvent.getEventTypeEnum();
            switch (eventType) {
                case DOLPHIN_SCHEDULER_PIPE_EXEC_EVENT:
                    String projectCode = resolvedEvent.getProjectCode();
                    String pipelineCode = resolvedEvent.getPipelineCode();
                    String cacheKey = projectCode + "_" + pipelineCode;
                    String taskCode = resolvedEvent.getTaskCode();
                    SchedTaskDefine define = cacheMap.computeIfAbsent(cacheKey, key -> {
                        List<SchedTaskDefine> schedTaskDefineList = globalService.getDolphinSchedulerInteractService()
                                .parsePipelineDefineByCode(projectCode, pipelineCode);
                        if (CollectionUtils.isEmpty(schedTaskDefineList)) {
                            return Collections.EMPTY_MAP;
                        }
                        return schedTaskDefineList.stream().collect(Collectors.toMap(
                                SchedTaskDefine::getTaskCode, Function.identity(), (o, n) -> n));
                    }).get(taskCode);
                    if (!Objects.isNull(define)) {
                        taskDefine.setRawScript(define.getRawScript());
                        taskDefine.setPreTaskCode(define.getPreTaskCode());
                    }
                    break;
                default:
                    taskDefine.setRawScript(eventType.getSummary());
            }
        }
        return ResponseResult.getSuccess(taskDefineList);
    }

    @Override
    public List<ExecutionFlowEntity> queryPrepareFlowList(List<Long> alreadyWaitFlowList) {
        final LambdaQueryWrapper<ExecutionFlowEntity> queryWrapper = new QueryWrapper<ExecutionFlowEntity>().lambda()
                .eq(ExecutionFlowEntity::getFlowStatus, FlowStatusEnum.PREPARE_EXECUTE);
        // .notIn(!CollectionUtils.isEmpty(alreadyWaitFlowList), ExecutionFlowEntity::getId, alreadyWaitFlowList);
        return list(queryWrapper);
    }

    private ExecutionFlowEntity getNewFlowEntity(DeployOneFlowReq req, String opUser,
                                                 @Nullable MetadataClusterData clusterDetail,
                                                 @Nullable MetadataComponentData componentDetail) {
        ExecutionFlowEntity executionFlowEntity = new ExecutionFlowEntity();
        BeanUtils.copyProperties(req, executionFlowEntity);
        executionFlowEntity.setId(req.getFlowId());
        executionFlowEntity.setCurBatchId(1);
        executionFlowEntity.setOperator(opUser);
        executionFlowEntity.setFlowRemark(req.getRemark());

        executionFlowEntity.setOrderNo(Constants.EMPTY_STRING);
        executionFlowEntity.setOrderId(Constants.EMPTY_STRING);

        if (!Objects.isNull(clusterDetail)) {
            executionFlowEntity.setClusterName(clusterDetail.getClusterName());
            executionFlowEntity.setRoleName(clusterDetail.getUpperService());
        }
        if (!Objects.isNull(componentDetail)) {
            executionFlowEntity.setComponentName(componentDetail.getComponentName());
        }

        /**
         * 获取审批状态
         */
        FlowStatusEnum applyStatus = getNewlyFlowApplyStatus(req, opUser, executionFlowEntity, clusterDetail, componentDetail);
        executionFlowEntity.setFlowStatus(applyStatus);

        executionFlowEntity.setFlowRollbackType(FlowRollbackType.NONE);
        return executionFlowEntity;
    }

    private FlowStatusEnum getNewlyFlowApplyStatus(DeployOneFlowReq req, String opUser,
                                                   ExecutionFlowEntity executionFlowEntity,
                                                   @Nullable MetadataClusterData clusterDetail,
                                                   @Nullable MetadataComponentData componentDetail) {
        FlowDeployType deployType = req.getDeployType();
        final OaChangeInfo changeInfo = new OaChangeInfo();
        changeInfo.setEnv(SpringApplicationContext.getEnv());
        final StringBuilder detailBuilder = new StringBuilder();
        String releaseScopeTypeValue;
        FlowReleaseScopeType releaseScopeType;
        FlowDeployPackageType deployPackageType;
        String approverStr = req.getApprover();
        String isApproval = req.getIsApproval();

        OAForm oaForm;
        List<String> nodeList = req.getNodeList();
        switch (deployType) {
            case MODIFY_MONITOR_OBJECT:
            case SPARK_CLIENT_PACKAGE_DEPLOY:
            case SPARK_EXPERIMENT:
            case PRESTO_TIDE_OFF:
            case PRESTO_TIDE_ON:
            case PRESTO_TO_PRESTO_TIDE_OFF:
            case PRESTO_TO_PRESTO_TIDE_ON:
            case CK_TIDE_OFF:
            case CK_TIDE_ON:
            case PRESTO_FAST_SHRINK:
            case PRESTO_FAST_EXPANSION:
            case YARN_TIDE_SHRINK:
            case YARN_TIDE_EXPANSION:
            case TRINO_EXPERIMENT:
                return FlowStatusEnum.PREPARE_EXECUTE;
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
                SparkDeployFlowExtParams sparkDeployFlowExtParams = JSONUtil.toBean(req.getExtParams(), SparkDeployFlowExtParams.class);
                SparkDeployType sparkDeployType = sparkDeployFlowExtParams.getSparkDeployType();
                changeInfo.setChangeType(deployType.getDesc());
                changeInfo.setChangeComponent("Spark-Manager");
                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);

                detailBuilder.append("发布场景:  ").append(sparkDeployType.getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("发布范围:  ").append(releaseScopeType.getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("应用版本:  ").append(sparkDeployFlowExtParams.getTargetSparkVersion()).append(Constants.NEW_LINE);
                String sparkDeployRemark = sparkDeployFlowExtParams.getRemark();
                if (!StringUtils.isBlank(sparkDeployRemark)) {
                    detailBuilder.append("额外说明:  ").append(sparkDeployRemark).append(Constants.NEW_LINE);
                }
                detailBuilder.append("变更详情:  ").append(getSparkDeployFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());

                oaForm = oaService.submitUnifiedForm(opUser, sparkDeployFlowExtParams.getApproverList(), sparkDeployFlowExtParams.getCcList(), null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, getSparkDeployExecTime(),
                        "infra.alter.spark-manager", this::getSparkBlockApprovalInfo);

                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
                SparkVersionLockExtParams sparkVersionLockExtParams = JSONUtil.toBean(req.getExtParams(), SparkVersionLockExtParams.class);
                changeInfo.setChangeType(deployType.getDesc());
                changeInfo.setChangeComponent("Spark-Manager");

                if (deployType == FlowDeployType.SPARK_VERSION_LOCK) {
                    detailBuilder.append("选中的任务将执行版本锁定（不再参与版本升级）。").append(Constants.NEW_LINE);
                } else {
                    detailBuilder.append("选中的任务将解除版本锁定（继续参与版本升级）。").append(Constants.NEW_LINE);
                }
                String sparkVersionLockRemark = sparkVersionLockExtParams.getRemark();
                if (!StringUtils.isBlank(sparkVersionLockRemark)) {
                    detailBuilder.append("额外说明:").append(sparkVersionLockRemark).append(Constants.NEW_LINE);
                }
                detailBuilder.append("变更详情:  ").append(getSparkDeployFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());

                oaForm = oaService.submitUnifiedForm(opUser, sparkVersionLockExtParams.getApproverList(), sparkVersionLockExtParams.getCcList(), null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, getSparkDeployExecTime(),
                        "infra.alter.spark-manager", this::getSparkBlockApprovalInfo);

                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;

            case HBO_JOB_PARAM_RULE_UPDATE:
            case HBO_JOB_PARAM_RULE_DELETE:

                if (Constants.IS_APPROVAL_NO.equalsIgnoreCase(isApproval)) {
                    return FlowStatusEnum.APPROVAL_PASS;
                }

                HboJobParamsUpdateFlowExtParams hboJobParamsUpdateFlowExtParams = JSONUtil.toBean(req.getExtParams(), HboJobParamsUpdateFlowExtParams.class);
                CodeDiff codeDiff = null;
                changeInfo.setChangeType(deployType.getDesc());
                changeInfo.setChangeComponent("hbo");

                Map<String, String> addParamsMap = hboJobParamsUpdateFlowExtParams.getAddParamsMap();
                Map<String, String> removeParamsMap = hboJobParamsUpdateFlowExtParams.getRemoveParamsMap();

                if (deployType == FlowDeployType.HBO_JOB_PARAM_RULE_UPDATE) {
                    detailBuilder.append("参数变更，不存在的会新增，存在的会合并或者删除").append(Constants.NEW_LINE);
                    codeDiff = new CodeDiff();
                    HashMap<String, String> originalMap = new HashMap<>();
                    HashMap<String, String> modifyMap = new HashMap<>();
                    originalMap.putAll(removeParamsMap);
                    modifyMap.putAll(addParamsMap);
                    codeDiff.setOriginal(JSONUtil.formatJsonStr(JSONUtil.toJsonStr(originalMap)));
                    codeDiff.setModified(JSONUtil.formatJsonStr(JSONUtil.toJsonStr(modifyMap)));
                } else {
                    detailBuilder.append("删除任务").append(Constants.NEW_LINE);
                }
                detailBuilder.append("变更任务id列表").append(String.join(",", nodeList)).append(Constants.NEW_LINE);


                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);

                detailBuilder.append("变更详情:  ").append(getHboDeployFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());
                String approver = approverStr;

                oaForm = oaService.submitUnifiedForm(opUser, Arrays.asList(approver.split(Constants.COMMA)), Collections.emptyList(), codeDiff,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().toEpochSecond(ZoneOffset.of("+08")),
                        "infra.alter.spark-manager", this::getHboBlockApprovalInfo);

                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;

            case K8S_CAPACITY_EXPANSION:
            case K8S_ITERATION_RELEASE:
                String clusterEnvironment = clusterDetail.getClusterEnvironment();
//                测试集群没有审批
                if (EnvUtils.isUat(clusterEnvironment)) {
                    return FlowStatusEnum.APPROVAL_PASS;
                }

                String[] approvorList = clusterDetail.getOwner().split(Constants.COMMA);
                detailBuilder.append("服务:").append(executionFlowEntity.getRoleName()).append(Constants.NEW_LINE);
                detailBuilder.append("发布类型:").append(executionFlowEntity.getDeployType().getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("并发度:").append(executionFlowEntity.getParallelism()).append(Constants.NEW_LINE);
                detailBuilder.append("容错度:").append(executionFlowEntity.getTolerance()).append(Constants.NEW_LINE);
                detailBuilder.append("生效方式:").append(executionFlowEntity.getEffectiveMode().getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("是否重启:").append(executionFlowEntity.getRestart()).append(Constants.NEW_LINE);
                detailBuilder.append("分组方式 :").append(executionFlowEntity.getGroupType().getDesc()).append(Constants.NEW_LINE);

                Long packageId = executionFlowEntity.getPackageId();
                if (NumberUtils.isPositiveLong(packageId)) {
                    detailBuilder.append("安装包版本 :").append(packageId).append(Constants.NEW_LINE);
                }

                Long configId = executionFlowEntity.getConfigId();
                if (NumberUtils.isPositiveLong(configId)) {
                    detailBuilder.append("配置版本 :").append(configId).append(Constants.NEW_LINE);
                }

                if (!StringUtils.isEmpty(req.getRemark())) {
                    detailBuilder.append("操作原因: ").append(req.getRemark()).append(Constants.NEW_LINE);
                }

                //            主机大于1000则只显示前1000条
                int approvalShowSize = 1000;
                if (!Objects.isNull(nodeList) && !nodeList.isEmpty()) {
                    if (nodeList.size() > approvalShowSize) {
                        detailBuilder.append("操作节点: ")
                                .append(System.lineSeparator())
                                .append(String.join(Constants.COMMA, nodeList.subList(0, approvalShowSize)))
                                .append("...");
                    } else {
                        detailBuilder.append("操作节点: ")
                                .append(System.lineSeparator())
                                .append(String.join(Constants.COMMA, nodeList.toString()));
                    }
                    detailBuilder.append("等共计" + nodeList.size() + "台主机");
                }

                changeInfo.setChangeComponent(componentDetail.getComponentName());
                changeInfo.setEnv(active);
                changeInfo.setChangeType(deployType.getDesc());
                changeInfo.setRemark(detailBuilder.toString());

                oaForm = oaService.submitUnifiedForm(opUser, Arrays.asList(approvorList), Collections.emptyList(), null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().toEpochSecond(ZoneOffset.of("+08")),
                        "infra.alter.bmr-cluster-scheduler-2", this::getSreBlockApprovalInfo);
                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                SparkPeripheryComponentDeployFlowExtParams sparkPeripheryComponentDeployFlowExtParams = JSONUtil.toBean(req.getExtParams(), SparkPeripheryComponentDeployFlowExtParams.class);
                final SparkPeripheryComponent peripheryComponent = sparkPeripheryComponentDeployFlowExtParams.getPeripheryComponent();
                changeInfo.setChangeType(deployType.getDesc() + ": " + peripheryComponent.getDesc());
                changeInfo.setChangeComponent("Spark-Manager");
                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);


                detailBuilder.append("组件名称:  ").append(req.getComponentName()).append(Constants.NEW_LINE);
                detailBuilder.append("发布场景:  ").append(sparkPeripheryComponentDeployFlowExtParams.getSparkDeployType().getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("发布范围:  ").append(releaseScopeType.getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("应用版本:  ").append(sparkPeripheryComponentDeployFlowExtParams.getTargetVersion()).append(Constants.NEW_LINE);
                String deployRemark = sparkPeripheryComponentDeployFlowExtParams.getRemark();
                if (!StringUtils.isBlank(deployRemark)) {
                    detailBuilder.append("额外说明:  ").append(deployRemark).append(Constants.NEW_LINE);
                }
                detailBuilder.append("变更详情:  ").append(getSparkDeployFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());

                oaForm = oaService.submitUnifiedForm(opUser, sparkPeripheryComponentDeployFlowExtParams.getApproverList(), sparkPeripheryComponentDeployFlowExtParams.getCcList(), null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, getSparkDeployExecTime(),
                        "infra.alter.spark-manager", this::getSparkBlockApprovalInfo);

                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                // enforce set component name for prepare and resolve events
                executionFlowEntity.setComponentName(peripheryComponent.name());
                return FlowStatusEnum.UNDER_APPROVAL;
            case NNPROXY_DEPLOY:
                final NNProxyDeployFlowExtParams nnProxyDeployFlowExtParams = JSONUtil.toBean(req.getExtParams(), NNProxyDeployFlowExtParams.class);
                changeInfo.setChangeType(nnProxyDeployFlowExtParams.getSubDeployType().getDesc());
                changeInfo.setChangeComponent(Constants.NN_PROXY);
                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
                detailBuilder.append("发布场景:  ").append(nnProxyDeployFlowExtParams.getUrgencyType().getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("发布范围:  ").append(releaseScopeType.getDesc()).append(Constants.NEW_LINE);

                final String packageType = req.getDeployPackageType();
                Preconditions.checkState(StringUtils.isNotEmpty(packageType), "deployPackageType is blank");
                deployPackageType = FlowDeployPackageType.valueOf(packageType);
                detailBuilder.append("变更包类型:  ").append(deployPackageType.getDesc()).append(Constants.NEW_LINE);

                if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                        deployPackageType == FlowDeployPackageType.SERVICE_PACKAGE) {
                    final MetadataPackageData metadataPackageData = bmrMetadataService.queryPackageDetailById(req.getPackageId());
                    detailBuilder.append("安装包版本: ").append(metadataPackageData.getTagName()).append(Constants.NEW_LINE);
                }

                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
                if (releaseScopeType == FlowReleaseScopeType.GRAY_RELEASE) {
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.CONFIG_PACKAGE) {
                        ConfigDetailData configDetailData = bmrConfigService.queryConfigDetailById(req.getConfigId());
                        detailBuilder.append("配置包版本: ").append(configDetailData.getConfigVersionNumber()).append(Constants.NEW_LINE);
                    }
                } else {
                    if (deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                            deployPackageType == FlowDeployPackageType.CONFIG_PACKAGE) {
                        final List<ComponentConfInfo> confInfoList = nnProxyDeployFlowExtParams.getConfInfoList();
                        detailBuilder.append("### 组件配置包版本列表: ###").append(Constants.NEW_LINE);
                        for (ComponentConfInfo componentConfInfo : confInfoList) {
                            detailBuilder.append(componentConfInfo.getComponentName()).append(":").append(componentConfInfo.getConfigVersion()).append(Constants.NEW_LINE);
                        }
                    }
                }
                detailBuilder.append("变更详情:  ").append(getSchedulerProxyFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());

                String[] nnproxyDeployApprovers = hdfsDeployApprovers;
                if (!Objects.isNull(clusterDetail) && StringUtils.isNotBlank(clusterDetail.getOwner())) {
                    nnproxyDeployApprovers = clusterDetail.getOwner().split(Constants.COMMA);
                    executionFlowEntity.setApprover(clusterDetail.getOwner());
                }
                oaForm = oaService.submitUnifiedForm(opUser, Arrays.asList(nnproxyDeployApprovers), null, null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().toEpochSecond(ZoneOffset.of("+08")),
                        "infra.alter.bmr-cluster-scheduler-2", this::getHdfsBlockApprovalInfo);

                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;

            case NNPROXY_RESTART:
                changeInfo.setChangeType(deployType.getDesc());
                changeInfo.setChangeComponent(Constants.NN_PROXY);
                releaseScopeTypeValue = req.getReleaseScopeType();
                releaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
                detailBuilder.append("服务: ").append(executionFlowEntity.getRoleName()).append(Constants.NEW_LINE);
                detailBuilder.append("集群:  ").append(clusterDetail.getClusterName()).append(Constants.NEW_LINE);
                detailBuilder.append("发布范围:  ").append(releaseScopeType.getDesc()).append(Constants.NEW_LINE);
                detailBuilder.append("变更详情:  ").append(getSchedulerFlowUrl(req.getFlowId()));
                changeInfo.setRemark(detailBuilder.toString());
                String[] nnproxyRestartApprovers = hdfsDeployApprovers;
                if (!Objects.isNull(clusterDetail) && StringUtils.isNotBlank(clusterDetail.getOwner())) {
                    nnproxyRestartApprovers = clusterDetail.getOwner().split(Constants.COMMA);
                    executionFlowEntity.setApprover(clusterDetail.getOwner());
                }
                oaForm = oaService.submitUnifiedForm(opUser, Arrays.asList(nnproxyRestartApprovers), null, null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, LocalDateTime.now().toEpochSecond(ZoneOffset.of("+08")),
                        "infra.alter.bmr-cluster-scheduler-2", this::getHdfsBlockApprovalInfo);
                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());
                return FlowStatusEnum.UNDER_APPROVAL;
            default:
                Boolean approval = Constants.TRUE.equals(req.getIsApproval());
                if (approval) {
                    return FlowStatusEnum.PREPARE_EXECUTE;
                }

                if (StringUtils.isBlank(approverStr)) {
                    throw new IllegalArgumentException("no approver ,please assign");
                }

                changeInfo.setChangeType(req.getDeployType().getDesc());
                changeInfo.setChangeComponent(componentDetail.getComponentName());
                changeInfo.setRemark(generateOaInfo(req, executionFlowEntity, nodeList));

                oaForm = oaService.submitUnifiedForm(opUser, Arrays.asList(approverStr.split(Constants.COMMA)), Collections.emptyList(), null,
                        Constants.BMR_UNIFIED_OA_PROCESS_NAME, changeInfo, getSparkDeployExecTime(),
                        "infra.alter.bmr-cluster-scheduler-2", this::getSreBlockApprovalInfo);
                executionFlowEntity.setOrderId(oaForm.getOrderId());
                executionFlowEntity.setOrderNo(oaForm.getOrderNo());

                return FlowStatusEnum.UNDER_APPROVAL;
        }
    }

    private String generateOaInfo(DeployOneFlowReq deployOneFlowReq, ExecutionFlowEntity executionFlow, List<String> nodeList) {
        Long packageId = deployOneFlowReq.getPackageId();
        MetadataPackageData metadataPackageData = null;

        Long configId = deployOneFlowReq.getConfigId();
        ConfigDetailData configDetailData = null;

        String roleName = executionFlow.getRoleName();
        String clusterName = executionFlow.getClusterName();
        String componentName = executionFlow.getComponentName();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("服务: ").append(roleName).append(System.lineSeparator()).append(System.lineSeparator()).append("集群名: ").append(clusterName).append(System.lineSeparator()).append(System.lineSeparator()).append("组件名称: ").append(componentName).append(System.lineSeparator()).append(System.lineSeparator());
        if (deployOneFlowReq.getDeployType().isReleaseType()) {
            stringBuilder.append("发布类型: ").append(deployOneFlowReq.getDeployType().getDesc()).append(System.lineSeparator()).append(System.lineSeparator());
        } else {
            stringBuilder.append("启停类型: ").append(deployOneFlowReq.getDeployType().getDesc()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        try {
            if (!Objects.isNull(deployOneFlowReq.getParallelism())) {
                stringBuilder.append("并发度: ").append(deployOneFlowReq.getParallelism()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            if (!Objects.isNull(deployOneFlowReq.getTolerance())) {
                stringBuilder.append("容错度: ").append(deployOneFlowReq.getTolerance()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            if (!Objects.isNull(deployOneFlowReq.getEffectiveMode())) {
                stringBuilder.append("生效方式: ").append(deployOneFlowReq.getEffectiveMode().getDesc()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            if (!Objects.isNull(deployOneFlowReq.getRestart())) {
                stringBuilder.append("是否重启: ").append(deployOneFlowReq.getRestart()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            if (!Objects.isNull(deployOneFlowReq.getGroupType())) {
                stringBuilder.append("分组方式: ").append(deployOneFlowReq.getGroupType().getDesc()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            try {
                if (NumberUtils.isPositiveLong(packageId)) {
                    metadataPackageData = bmrMetadataService.queryPackageDetailById(packageId);
                    stringBuilder.append("安装包版本: ").append(metadataPackageData.getTagName()).append(System.lineSeparator()).append(System.lineSeparator());
                }
                if (NumberUtils.isPositiveLong(configId)) {
                    configDetailData = bmrConfigService.queryConfigDetailById(configId);
                    stringBuilder.append("配置版本: ").append(configDetailData.getConfigVersionNumber()).append(System.lineSeparator()).append(System.lineSeparator());
                }
            } catch (Exception e) {
                log.error("get approve package and config version error: " + e.getMessage());
            }
            if (!Objects.isNull(deployOneFlowReq.getRemark())) {
                stringBuilder.append("操作原因: ").append(deployOneFlowReq.getRemark()).append(System.lineSeparator()).append(System.lineSeparator());
            }

//            主机大于1000则只显示前1000条
            int approvalShowSize = 1000;
            if (!Objects.isNull(nodeList) && !nodeList.isEmpty()) {
                if (nodeList.size() > approvalShowSize) {
                    stringBuilder.append("操作节点: ")
                            .append(System.lineSeparator())
                            .append(String.join(Constants.COMMA, nodeList.subList(0, approvalShowSize)))
                            .append("...");
                } else {
                    stringBuilder.append("操作节点: ")
                            .append(System.lineSeparator())
                            .append(String.join(Constants.COMMA, nodeList.toString()));
                }
                stringBuilder.append("等共计" + nodeList.size() + "台主机");
            }

        } catch (Exception e) {
            log.error("set approve params error: " + e.getMessage());
        }

        return stringBuilder.toString();

    }

    private long getSparkDeployExecTime() {
        final LocalDateTime now = LocalDateTime.now();
        final int hour = now.getHour();
        // 夜间跳过封网审批
        if (hour >= 23 || hour <= 8) {
            return LocalDateFormatterUtils.parseByPattern(Constants.FMT_DATE_TIME,
                    "2025-06-26 12:00:00").toEpochSecond(ZoneOffset.of("+08"));
        } else {
            return now.toEpochSecond(ZoneOffset.of("+08"));
        }
    }

    private String getSparkDeployFlowUrl(long flowId) {
        String domainPrefix = active.equalsIgnoreCase("pre") ? "pre-" : "";
        String url = String.format("http://%sbmr.bilibili.co/bmr/spark/publishManager/releaseTask?flowId=%s", domainPrefix, flowId);
        return url;
    }

    private String getHboDeployFlowUrl(long flowId) {
        String domainPrefix = active.equalsIgnoreCase("pre") ? "pre-" : "";
        String url = String.format("http://%sbmr.bilibili.co/bmr/hbo/publish/releaseTask?flowId=%s", domainPrefix, flowId);
        return url;
    }

    private String getSchedulerFlowUrl(long flowId) {
        String domainPrefix = active.equalsIgnoreCase("pre") ? "pre-" : "";
        String url = String.format("http://%sbmr.bilibili.co/bmr/deployorStop/deploy/deployTaskForm?flowId=%s", domainPrefix, flowId);
        return url;
    }

    private String getSchedulerProxyFlowUrl(long flowId) {
        String domainPrefix = active.equalsIgnoreCase("pre") ? "pre-" : "";
        String url = String.format("http://%sbmr.bilibili.co/bmr/deployorStop/deploy/deployProxyTaskForm?flowId=%s", domainPrefix, flowId);
        return url;
    }

    private ReplaceRoleModel getSparkBlockApprovalInfo() {
        final ReplaceRoleModel replaceRoleModel = new ReplaceRoleModel();
        replaceRoleModel.setDevLeader(sparkDevLeaders);
        replaceRoleModel.setSreLeader(sparkSreLeaders);
        return replaceRoleModel;
    }

    private ReplaceRoleModel getHdfsBlockApprovalInfo() {
        final ReplaceRoleModel replaceRoleModel = new ReplaceRoleModel();
        replaceRoleModel.setDevLeader(hdfsDevLeaders);
        replaceRoleModel.setSreLeader(hdfsSreLeaders);
        return replaceRoleModel;
    }

    private ReplaceRoleModel getHboBlockApprovalInfo() {
        final ReplaceRoleModel replaceRoleModel = new ReplaceRoleModel();
        replaceRoleModel.setDevLeader(hboDevLeaders);
        replaceRoleModel.setSreLeader(hboSreLeaders);
        return replaceRoleModel;
    }

    private ReplaceRoleModel getSreBlockApprovalInfo() {
        final ReplaceRoleModel replaceRoleModel = new ReplaceRoleModel();
        replaceRoleModel.setDevLeader(sreDevLeaders);
        replaceRoleModel.setSreLeader(sreSreLeaders);
        return replaceRoleModel;
    }

    @Override
    public ResponseResult queryFlowPage(QueryFlowPageReq req) {
        Page<ExecutionFlowEntity> page = new Page<>(req.getPageNum(), req.getPageSize());

        IPage<ExecutionFlowEntity> pageList = baseMapper.selectPageList(page, req);
        return ResponseResult.getSuccess(pageList);
    }

    @Override
    public ResponseResult getFlowRuntimeData(Long flowId) {
        ExecutionFlowEntity executionFlow = getById(flowId);
        if (executionFlow == null) {
            return ResponseResult.getError("发布变更任务不存在");
        }

        ExecutionFlowRuntimeDataDTO flowRuntimeData = new ExecutionFlowRuntimeDataDTO();
        flowRuntimeData.setFlowStatus(executionFlow.getFlowStatus());
        flowRuntimeData.setFlowEntity(executionFlow);
        flowRuntimeData.setFlowId(executionFlow.getId());

        List<ExecutionNodeSummary> executionNodeSummaryList = executionNodeService.queryExecutionNodeSummary(flowId);
        // String curStage = executionNodeService.queryCurStage(flowId, executionFlow.getCurBatchId());
        final ExecutionNodeEntity curNodeEntity = executionNodeService.queryCurExecOneNode(flowId, executionFlow.getCurBatchId());
        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowIdWithCache(flowId, BaseFlowExtPropDTO.class);
        if (!Objects.isNull(baseFlowExtPropDTO)) {
            flowRuntimeData.setAllowedNextProceedTime(baseFlowExtPropDTO.getAllowedNextProceedTime());
        }
        FlowDetailAdaptor.statisticalStatus(executionFlow, executionNodeSummaryList, flowRuntimeData, curNodeEntity, baseFlowExtPropDTO);

        String log = executionLogService.queryLogByExecuteId(flowId, LogTypeEnum.FLOW);
        flowRuntimeData.setFlowLog(log);

        fillButton(flowRuntimeData);

        final FlowDeployType deployType = executionFlow.getDeployType();
        RichedExecutionFlowRuntimeDataDTO richedExecutionFlowRuntimeDataDTO;
        switch (deployType) {
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, SparkDeployFlowExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, SparkVersionLockExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case SPARK_CLIENT_PACKAGE_DEPLOY:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, SparkClientDeployExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case SPARK_EXPERIMENT:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, SparkExperimentFlowExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case HBO_JOB_PARAM_RULE_UPDATE:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, HboJobParamsUpdateFlowExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case PRESTO_TIDE_OFF:
            case PRESTO_TIDE_ON:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, PrestoTideExtFlowParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, SparkPeripheryComponentDeployFlowExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            case NNPROXY_DEPLOY:
                richedExecutionFlowRuntimeDataDTO = fillFlowExtParams(flowRuntimeData, NNProxyDeployFlowExtParams.class);
                return ResponseResult.getSuccess(richedExecutionFlowRuntimeDataDTO);
            default:
                return ResponseResult.getSuccess(flowRuntimeData);
        }
    }

    private <T> RichedExecutionFlowRuntimeDataDTO<T> fillFlowExtParams(ExecutionFlowRuntimeDataDTO flowRuntimeData, Class<T> clazz) {
        final RichedExecutionFlowRuntimeDataDTO<T> richedExecutionFlowRuntimeDataDTO = new RichedExecutionFlowRuntimeDataDTO();
        BeanUtils.copyProperties(flowRuntimeData, richedExecutionFlowRuntimeDataDTO);
        T flowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flowRuntimeData.getFlowId(), clazz);
        richedExecutionFlowRuntimeDataDTO.setExtParams(flowExtParams);
        return richedExecutionFlowRuntimeDataDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult alterFlowStatus(long flowId, FlowOperateButtonEnum operate) throws Exception {
        try {
            ExecutionFlowEntity executionFlow = getById(flowId);
            if (Objects.isNull(executionFlow)) {
                return ResponseResult.getError("工作流不存在");
            }
            FlowStatusEnum currentStatus = executionFlow.getFlowStatus();


            if (!currentStatus.isRunning()) {
                if (currentStatus == FlowStatusEnum.APPROVAL_PASS && operate == FlowOperateButtonEnum.PROCEED) {
                    return executionOneFlow(flowId);
                }
                return ResponseResult.getError("工作流非running状态无法变更状态");
            }
            FlowStatusEnum nextStatus = currentStatus.generateNextStatus(operate);
            if (Objects.isNull(nextStatus)) {
                return ResponseResult.getError(String.format("不支持的变更操作,状态:%s，操作:%s", currentStatus.getDesc(), operate.getDesc()));
            }
            executionLogService.updateLogContent(flowId, LogTypeEnum.FLOW, String.format("user %s update flow status, before status is %s, after status is %s, operate is %s", MDC.get(Constants.REQUEST_USER), currentStatus.getDesc(), nextStatus.getDesc(), operate.getDesc()));

            UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
            updateExecutionFlowDTO.setFlowId(flowId);
            updateExecutionFlowDTO.setFlowStatus(nextStatus);
            updateFlow(updateExecutionFlowDTO);
            bmrFlowService.alterFlowStatus(flowId, operate);

            switch (operate) {
                case PROCEED:
                    checkAllowedClickProceed(flowId);
                    handleProcessWhenRollback(flowId);
                    break;
                case TERMINATE:
                    processInstanceExecCacheManager.removeByProcessInstanceId(flowId);
                    executionFlowAopEventService.finishFlowAop(executionFlow, currentStatus);
                    break;
                case SKIP_FAILED_AND_PROCESS:
                    executionNodeService.updateNodeStatusByFlowIdAndNodeStatus(flowId, NodeExecuteStatusEnum.FAIL_NODE_EXECUTE, NodeExecuteStatusEnum.FAIL_SKIP_NODE_EXECUTE);
                    executionNodeService.updateNodeStatusByFlowIdAndNodeStatus(flowId, NodeExecuteStatusEnum.FAIL_NODE_RETRY_EXECUTE, NodeExecuteStatusEnum.FAIL_SKIP_NODE_RETRY_EXECUTE);
                    executionNodeService.updateNodeStatusByFlowIdAndNodeStatus(flowId, NodeExecuteStatusEnum.FAIL_NODE_ROLLBACK_EXECUTE, NodeExecuteStatusEnum.FAIL_SKIP_NODE_ROLLBACK_EXECUTE);
                    // 更新并重置当前失败节点数量
                    executionFlowService.updateCurFault(flowId, 0);
                    break;
                case STAGED_ROLLBACK:
                case FULL_ROLLBACK:
                    rollbackBusFactoryService.getRollbackFactory(executionFlow)
                            .doRollback(operate, executionFlow);
                    break;
            }
            return ResponseResult.getSuccess();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new Exception(e.getMessage(), e);
        }
    }

    private void checkAllowedClickProceed(long flowId) {
        final BaseFlowExtPropDTO baseFlowExtPropDTO = executionFlowPropsService.getFlowPropByFlowIdWithCache(flowId, BaseFlowExtPropDTO.class);
        if (Objects.isNull(baseFlowExtPropDTO)) {
            return;
        }
        final String allowedNextProceedTime = baseFlowExtPropDTO.getAllowedNextProceedTime();
        if (StringUtils.isBlank(allowedNextProceedTime)) {
            return;
        }
        String nowFmt = LocalDateFormatterUtils.getNowDefaultFmt();

        // 如果当前时间小于允许点击继续执行时间
        if (nowFmt.compareTo(allowedNextProceedTime) < 0) {
            throw new IllegalArgumentException("now not allowed click proceed, until time: " + allowedNextProceedTime);
        }
    }

    private boolean allowedClickProceed(long flowId) {
        try {
            checkAllowedClickProceed(flowId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void handleProcessWhenRollback(long flowId) {
        ExecutionFlowEntity executionFlow = getById(flowId);
        Preconditions.checkState(FlowStatusEnum.IN_EXECUTE.equals(executionFlow.getFlowStatus()),
                "工作流状态需要为: [IN_EXECUTE] 状态，当前为: " + executionFlow.getFlowStatus());
        // executionFlowService.updateFlowRollbackType(FlowRollbackType.STAGE, flowId);

        final FlowRollbackType flowRollbackType = executionFlow.getFlowRollbackType();
        if (!flowRollbackType.equals(FlowRollbackType.NONE)) {
            executionFlowService.updateFlowRollbackType(FlowRollbackType.NONE, flowId);
        }
//        Preconditions.checkState(flowRollbackType == FlowRollbackType.NONE, "require flow rollback type is none");
        final Integer curBatchId = executionFlow.getCurBatchId();

        // 对处于rollback状态节点切换至等待正常执行状态
        final LambdaUpdateWrapper<ExecutionNodeEntity> updateWrapper = new UpdateWrapper<ExecutionNodeEntity>().lambda()
                .eq(ExecutionNodeEntity::getFlowId, flowId)
                .eq(ExecutionNodeEntity::getDeleted, 0)
                .ge(ExecutionNodeEntity::getBatchId, curBatchId)
                .eq(ExecutionNodeEntity::getExecType, NodeExecType.ROLLBACK)
                .set(ExecutionNodeEntity::getExecType, NodeExecType.WAITING_FORWARD);
        executionNodeService.update(updateWrapper);
    }

    @Override
    public ResponseResult queryFlowInfo(Long flowId) {
        ExecutionFlowEntity executionFlow = getById(flowId);
        if (Objects.isNull(executionFlow)) {
            return ResponseResult.getError("工作流不存在");
        }
        return ResponseResult.getSuccess(executionFlow);
    }

    @Override
    public void updateCurFault(Long flowId, Integer curFault) {
        executionFlowMapper.updateCurFault(flowId, curFault);
    }

    private void fillButton(ExecutionFlowRuntimeDataDTO flowRuntimeData) {
        FlowStatusEnum flowStatus = flowRuntimeData.getFlowStatus();
        List<OpStrategyButton> opStrategyButtonList = new ArrayList<>();
        OpStrategyButton proceed = getButtonAndPutInList(FlowOperateButtonEnum.PROCEED, opStrategyButtonList);
        OpStrategyButton pause = getButtonAndPutInList(FlowOperateButtonEnum.PAUSE, opStrategyButtonList);
        OpStrategyButton skipFailAndProceed = getButtonAndPutInList(FlowOperateButtonEnum.SKIP_FAILED_AND_PROCESS, opStrategyButtonList);
        OpStrategyButton terminate = getButtonAndPutInList(FlowOperateButtonEnum.TERMINATE, opStrategyButtonList);

        // 按需填充回滚按钮
        fillButtonWithRollback(flowRuntimeData, opStrategyButtonList);

        for (OpStrategyButton opStrategyButton : opStrategyButtonList) {
            FlowStatusEnum nextStatus = flowStatus.generateNextStatus(opStrategyButton.getButton());
            if (!Objects.isNull(nextStatus)) {
                opStrategyButton.setState(true);
            }
        }

        if (!allowedClickProceed(flowRuntimeData.getFlowId())) {
            proceed.setState(false);
        }
        flowRuntimeData.setButtonList(opStrategyButtonList);
    }

    private void fillButtonWithRollback(ExecutionFlowRuntimeDataDTO flowRuntimeData, List<OpStrategyButton> opStrategyButtonList) {
        final ExecutionFlowEntity flowEntity = flowRuntimeData.getFlowEntity();

        final FlowRollbackFactory rollbackFactory = rollbackBusFactoryService.getRollbackFactory(flowEntity);
        boolean supportRollback = rollbackFactory.supportRollback(flowEntity);

        if (supportRollback) {
            List<FlowOperateButtonEnum> rollbackButtonList = rollbackFactory.getRollbackButton(flowEntity);
            for (FlowOperateButtonEnum flowOperateButtonEnum : rollbackButtonList) {
                getButtonAndPutInList(flowOperateButtonEnum, opStrategyButtonList);
            }
        }
    }

    private OpStrategyButton getButtonAndPutInList(FlowOperateButtonEnum button, List<OpStrategyButton> opStrategyButtonList) {
        OpStrategyButton opStrategyButton = new OpStrategyButton();
        opStrategyButton.setButton(button);
        opStrategyButtonList.add(opStrategyButton);
        return opStrategyButton;
    }

    private String generateOAFormDetail(ExecutionFlowEntity executionFlowEntity, DeployOneFlowReq req) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("服务: ").append(executionFlowEntity.getRoleName())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("集群名: ").append(executionFlowEntity.getClusterName())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("组件名称: ").append(executionFlowEntity.getComponentName())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
        ;
        FlowDeployType deployType = executionFlowEntity.getDeployType();
        if (deployType.isReleaseType()) {
            stringBuilder.append("发布类型: ").append(deployType.getDesc()).append(System.lineSeparator())
                    .append(System.lineSeparator());
        } else {
            stringBuilder.append("启停类型: ").append(deployType.getDesc()).append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        stringBuilder.append("并发度: ")
                .append(executionFlowEntity.getParallelism())
                .append(System.lineSeparator()).append(System.lineSeparator());
        stringBuilder.append("容错度: ")
                .append(executionFlowEntity.getTolerance())
                .append(System.lineSeparator()).append(System.lineSeparator());
        stringBuilder.append("生效方式: ")
                .append(executionFlowEntity.getEffectiveMode().getDesc())
                .append(System.lineSeparator()).append(System.lineSeparator());
        stringBuilder.append("是否重启: ")
                .append(executionFlowEntity.getRestart())
                .append(System.lineSeparator()).append(System.lineSeparator());
        stringBuilder.append("分组方式: ")
                .append(req.getGroupType().getDesc())
                .append(System.lineSeparator()).append(System.lineSeparator());
        stringBuilder.append("操作原因: ").append(req.getRemark())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        return stringBuilder.toString();
    }

    @Override
    public ResponseResult querySparkDeployFlowPageList(QuerySparkDeployFlowPageReq req) {
        Page<ExecutionFlowEntity> page = new Page<>(req.getPageNum(), req.getPageSize());

        IPage pageList = baseMapper.selectSparkDeployPageList(page, req);

        List<ExecutionFlowEntity> records = pageList.getRecords();
        List<SparkDeployFlowInfoDTO> collect = records.stream().map(flow -> getSparkDeployFlowInfoDTO(flow))
                .collect(Collectors.toList());
        pageList.setRecords(collect);
        return ResponseResult.getSuccess(pageList);
    }

    @Override
    public ResponseResult querySparkDeployFlowListByJobId(String jobId) {
        final ExecutionNodeEntity queryDo = new ExecutionNodeEntity();
        queryDo.setNodeName(jobId);
        List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeList(queryDo, false);
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }
        List<SparkDeployFlowInfoDTO> flowInfoDTOList = new ArrayList<>();
        for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
            long flowId = nodeEntity.getFlowId();
            ExecutionFlowEntity flow = getById(flowId);

            final FlowDeployType deployType = flow.getDeployType();
            SparkDeployFlowInfoDTO flowInfoDTO;
            switch (deployType) {
                case SPARK_DEPLOY:
                case SPARK_DEPLOY_ROLLBACK:
                    flowInfoDTO = getSparkDeployFlowInfoDTO(flow);
                    SparkDeployFlowExtParams sparkDeployFlowExtParams = (SparkDeployFlowExtParams) flowInfoDTO.getExtParams();
                    flowInfoDTO.setMajorSparkVersion(sparkDeployFlowExtParams.getMajorSparkVersion());
                    flowInfoDTO.setTargetSparkVersion(sparkDeployFlowExtParams.getTargetSparkVersion());
                    flowInfoDTOList.add(flowInfoDTO);
                    break;
                case SPARK_VERSION_LOCK:
                case SPARK_VERSION_RELEASE:
                    flowInfoDTO = getSparkDeployFlowInfoDTO(flow);
                    flowInfoDTOList.add(flowInfoDTO);
                    final SparkDeployJobExtParams sparkVersionJobExtParams = nodePropsService.queryNodePropsByNodeId(nodeEntity.getId(), SparkDeployJobExtParams.class);
                    if (Objects.isNull(sparkVersionJobExtParams)) {
                        break;
                    }
                    final String targetSparkVersion = sparkVersionJobExtParams.getTargetSparkVersion();
                    if (!StringUtils.isBlank(targetSparkVersion)) {
                        if (targetSparkVersion.contains("v3.")) {
                            flowInfoDTO.setMajorSparkVersion("v3.1.1-bilibili");
                        } else {
                            flowInfoDTO.setMajorSparkVersion("v4.0.0-bilibili");
                        }
                    }
                    flowInfoDTO.setTargetSparkVersion(targetSparkVersion);
                    break;
            }
        }
        return ResponseResult.getSuccess(flowInfoDTOList);
    }

    private SparkDeployFlowInfoDTO getSparkDeployFlowInfoDTO(ExecutionFlowEntity flow) {
        final SparkDeployFlowInfoDTO flowInfoDTO = new SparkDeployFlowInfoDTO();
        flowInfoDTO.setFlowId(flow.getId());
        flowInfoDTO.setDeployType(flow.getDeployType());
        flowInfoDTO.setFlowStatus(flow.getFlowStatus());
        flowInfoDTO.setOpUser(flow.getOperator());
        flowInfoDTO.setStartTime(flow.getStartTime());
        flowInfoDTO.setEndTime(flow.getEndTime());
        flowInfoDTO.setCtime(flow.getCtime());
        flowInfoDTO.setMtime(flow.getMtime());
        flowInfoDTO.setOrderId(flow.getOrderId());
        flowInfoDTO.setOrderNo(flow.getOrderNo());

        final FlowDeployType deployType = flow.getDeployType();
        switch (deployType) {
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
                SparkDeployFlowExtParams sparkDeployFlowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(),
                        SparkDeployFlowExtParams.class);
                flowInfoDTO.setExtParams(sparkDeployFlowExtParams);
                break;
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
                SparkVersionLockExtParams sparkVersionLockExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(),
                        SparkVersionLockExtParams.class);
                flowInfoDTO.setExtParams(sparkVersionLockExtParams);
                break;
            case SPARK_EXPERIMENT:
                SparkExperimentFlowExtParams sparkExperimentFlowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(),
                        SparkExperimentFlowExtParams.class);
                flowInfoDTO.setExtParams(sparkExperimentFlowExtParams);
                break;
            case SPARK_CLIENT_PACKAGE_DEPLOY:
                final SparkClientDeployExtParams sparkClientDeployExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(),
                        SparkClientDeployExtParams.class);
                flowInfoDTO.setExtParams(sparkClientDeployExtParams);
                break;
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                final SparkPeripheryComponentDeployFlowExtParams sparkPeripheryComponentDeployFlowExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(), SparkPeripheryComponentDeployFlowExtParams.class);
                flowInfoDTO.setExtParams(sparkPeripheryComponentDeployFlowExtParams);
                break;
        }
        return flowInfoDTO;
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public ExecutionFlowEntity queryByIdWithTransactional(Long flowId) {
        return baseMapper.selectById(flowId);
    }

    @Override
    public ResponseResult queryFlowInfoList(QueryFlowListReq req) {
        final List<Long> flowIdList = req.getFlowIdList();
        if (CollectionUtils.isEmpty(flowIdList)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }

        final LambdaQueryWrapper<ExecutionFlowEntity> queryWrapper = new QueryWrapper<ExecutionFlowEntity>().lambda().eq(ExecutionFlowEntity::getDeleted, 0)
                .in(ExecutionFlowEntity::getId, flowIdList);
        final List<ExecutionFlowEntity> flowEntityList = list(queryWrapper);
        return ResponseResult.getSuccess(flowEntityList);
    }

    @Override
    public ResponseResult querySparkClientDeployInfoByNodeName(String nodeName) {
        final ExecutionNodeEntity queryDo = new ExecutionNodeEntity();
        queryDo.setNodeName(nodeName);
        List<ExecutionNodeEntity> nodeEntityList = executionNodeService.queryNodeList(queryDo, false);
        if (CollectionUtils.isEmpty(nodeEntityList)) {
            return ResponseResult.getSuccess(Collections.emptyList());
        }

        List<SparkClientDeployHistoryDTO> historyDTOS = new ArrayList<>();
        Set<Long> removedVersions = new HashSet<>();
        for (ExecutionNodeEntity nodeEntity : nodeEntityList) {
            long flowId = nodeEntity.getFlowId();
            ExecutionFlowEntity flow = getById(flowId);

            final FlowDeployType deployType = flow.getDeployType();
            if (deployType != FlowDeployType.SPARK_CLIENT_PACKAGE_DEPLOY) {
                continue;
            }
            SparkClientDeployExtParams deployExtParams = executionFlowPropsService.getFlowExtParamsByCache(flow.getId(), SparkClientDeployExtParams.class);
            final SparkClientDeployType packDeployType = deployExtParams.getPackDeployType();
            if (packDeployType == SparkClientDeployType.REMOVE_USELESS_HOSTS) {
                break;
            }
            List<Long> packIdList = deployExtParams.getPackIdList();
            if (CollectionUtils.isEmpty(packIdList)) {
                continue;
            }

            if (packDeployType == SparkClientDeployType.REMOVE_USELESS_VERSION) {
                removedVersions.addAll(packIdList);
                continue;
            }

            for (Long packId : packIdList) {
                if (removedVersions.contains(packId)) {
                    continue;
                }
                MetadataPackageData metadataPackageData;
                try {
                    metadataPackageData = bmrMetadataService.queryPackageDetailById(packId);
                } catch (Exception e) {
                    log.error("query pack detail info error, case: " + e.getMessage(), e);
                    continue;
                }
                if (Objects.isNull(metadataPackageData)) {
                    continue;
                }
                final SparkClientDeployHistoryDTO historyDTO = new SparkClientDeployHistoryDTO();
                historyDTO.setCtime(flow.getCtime());
                historyDTO.setTagName(metadataPackageData.getTagName());
                final String componentName = metadataPackageData.getComponentName();
                final SparkClientType clientType = SparkClientType.getByComponentName(componentName);
                historyDTO.setClientType(clientType);
                historyDTOS.add(historyDTO);
            }
        }
        return ResponseResult.getSuccess(historyDTOS);
    }

    @Override
    public ResponseResult deleteFlowById(Long flowId) {
        final LambdaUpdateWrapper<ExecutionFlowEntity> queryWrapper = new UpdateWrapper<ExecutionFlowEntity>().lambda()
                .eq(ExecutionFlowEntity::getDeleted, 0)
                .eq(ExecutionFlowEntity::getId, flowId)
                .set(ExecutionFlowEntity::getDeleted, 1);
        boolean state = update(queryWrapper);
        return ResponseResult.getSuccess(state);
    }

    @Override
    public ResponseResult adminModifyFlowStatus(Long flowId, String flowState) {
        Preconditions.checkState(isAdminRole(), "not admin role, can not modify flow status!");
        FlowStatusEnum flowStatusEnum = FlowStatusEnum.valueOf(flowState);
        updateFlowStatusByFlowId(flowId, flowStatusEnum);
        return ResponseResult.getSuccess("update success");
    }

    @Override
    public List<String> getOpAdminList() {
        return opAdminList;
    }

    @Override
    public boolean updateFlowTolerance(Long flowId, int tolerance) {
        final LambdaUpdateWrapper<ExecutionFlowEntity> updateWrapper = new UpdateWrapper<ExecutionFlowEntity>().lambda()
                .eq(ExecutionFlowEntity::getDeleted, 0)
                .eq(ExecutionFlowEntity::getId, flowId)
                .set(ExecutionFlowEntity::getTolerance, tolerance);
        return update(updateWrapper);
    }

    @Override
    public boolean updateFlowRollbackType(FlowRollbackType rollbackType, long flowId) {
        final LambdaUpdateWrapper<ExecutionFlowEntity> updateWrapper = new UpdateWrapper<ExecutionFlowEntity>().lambda()
                .eq(ExecutionFlowEntity::getDeleted, 0)
                .eq(ExecutionFlowEntity::getId, flowId)
                .set(ExecutionFlowEntity::getFlowRollbackType, rollbackType);
        return update(updateWrapper);
    }

    @Override
    public boolean updateCurrentBatchIdByFlowId(Long flowId, Integer batchId) {
        UpdateExecutionFlowDTO updateExecutionFlowDTO = new UpdateExecutionFlowDTO();
        updateExecutionFlowDTO.setFlowId(flowId);
        updateExecutionFlowDTO.setCurrentBatchId(batchId);
        executionFlowMapper.updateFlow(updateExecutionFlowDTO);
        return true;
    }

    @Override
    public String generateFlowUrl(Long flowId) {
        ExecutionFlowEntity executionFlow = executionFlowService.getById(flowId);
        return generateFlowUrl(executionFlow);
    }

    @Override
    public String generateFlowUrl(ExecutionFlowEntity executionFlow) {
        Assert.notNull(executionFlow, "executionFlow is null");
        // http://pre-bmr.bilibili.co/bmr/spark/publishManager/releaseTask?flowId=2744
        // https://pre-cloud-bm.bilibili.co/bmr/deployorStop/deploy/deployTaskForm?flowId=2640
        final FlowDeployType deployType = executionFlow.getDeployType();
        String path;

        switch (deployType) {
            case SPARK_DEPLOY:
            case SPARK_DEPLOY_ROLLBACK:
            case SPARK_VERSION_LOCK:
            case SPARK_VERSION_RELEASE:
            case SPARK_CLIENT_PACKAGE_DEPLOY:
            case SPARK_EXPERIMENT:
            case SPARK_PERIPHERY_COMPONENT_DEPLOY:
            case SPARK_PERIPHERY_COMPONENT_ROLLBACK:
            case SPARK_PERIPHERY_COMPONENT_LOCK:
            case SPARK_PERIPHERY_COMPONENT_RELEASE:
                path = "/bmr/spark/publishManager/releaseTask?flowId=";
                break;
            case HBO_JOB_PARAM_RULE_UPDATE:
            case HBO_JOB_PARAM_RULE_DELETE:
                path = "/bmr/hbo/publish/releaseTask?flowId=";
                break;
            case NNPROXY_DEPLOY:
                path = "/bmr/deployorStop/deploy/deployProxyTaskForm?flowId=";
                break;
            default:
                path = "/bmr/deployorStop/deploy/deployTaskForm?flowId=";
                break;
        }

        String domain;
        if ("prod".equalsIgnoreCase(active)) {
            domain = "http://bmr.bilibili.co";

        } else {
            domain = "http://pre-bmr.bilibili.co";

        }

        String suffix = "&deployType=" + executionFlow.getDeployType().name();

        String url = domain + path + executionFlow.getId() + suffix;
        return url;
    }

    private boolean isAdminRole(String opUser) {
        return opAdminList.contains(opUser);
    }

    private boolean isAdminRole() {
        return isAdminRole(MDC.get(Constants.REQUEST_USER));
    }

}
