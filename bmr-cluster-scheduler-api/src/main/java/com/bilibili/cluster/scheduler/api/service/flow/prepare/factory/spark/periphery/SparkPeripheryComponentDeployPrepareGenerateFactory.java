package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.spark.periphery;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.bean.SpringApplicationContext;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.redis.RedisService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponent;
import com.bilibili.cluster.scheduler.common.dto.spark.periphery.SparkPeripheryComponentDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.utils.CacheUtils;
import com.bilibili.cluster.scheduler.common.utils.StageSplitUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class SparkPeripheryComponentDeployPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    RedisService redisService;

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    BmrMetadataService metadataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();
        BaseFlowExtPropDTO flowProps = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        String flowExtParamsValue = flowProps.getFlowExtParams();

        String cacheKey = CacheUtils.getFlowExtParamsCacheKey(SpringApplicationContext.getEnv(), flowId);
        redisService.set(cacheKey, flowExtParamsValue, Constants.ONE_MINUTES * 60);
        SparkPeripheryComponentDeployFlowExtParams deployExtParams = JSONUtil.toBean(flowExtParamsValue, SparkPeripheryComponentDeployFlowExtParams.class);

        final String releaseScopeTypeValue = flowEntity.getReleaseScopeType();
        FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
        final FlowDeployType deployType = flowEntity.getDeployType();

        List<String> jobIdList = flowProps.getNodeList();
        String targetVersion = deployExtParams.getTargetVersion();
        final SparkPeripheryComponent peripheryComponent = deployExtParams.getPeripheryComponent();
        log.info("spark periphery component of {} deploy target version is {}", peripheryComponent, targetVersion);

        Map<String, Set<String>> stageWithJobs;
        final SparkDeployType sparkDeployType = deployExtParams.getSparkDeployType();
        List<Integer> percentStageList = deployExtParams.getPercentStageList();
        if (FlowReleaseScopeType.FULL_RELEASE == flowReleaseScopeType) {
            if (sparkDeployType == SparkDeployType.NORMAL) {
                stageWithJobs = sparkManagerService.queryAllReleaseStageWithJobs(peripheryComponent, targetVersion);
            } else {
                // 紧急发布
                percentStageList = Arrays.asList(100);
                jobIdList = sparkManagerService.queryAllReleaseJobList(peripheryComponent, targetVersion);
                stageWithJobs = StageSplitUtil.buildStageMap(jobIdList, percentStageList);
            }
            InstallationPackage installationPackage = metadataService.querySparkPeripheryComponentDefaultPackage(peripheryComponent.getAlias());
            if (!Objects.isNull(installationPackage)) {
                deployExtParams.setOriginalVersion(installationPackage.getMinorVersion());
                flowExtParamsValue = JSONUtil.toJsonStr(deployExtParams);
                redisService.set(cacheKey, flowExtParamsValue, Constants.ONE_MINUTES * 60);
            }
        } else {
            stageWithJobs = StageSplitUtil.buildStageMap(jobIdList, percentStageList);
        }

        if (MapUtils.isEmpty(stageWithJobs)) {
            log.info("Spark periphery component of {}, not find any job require deploy with target version {}", peripheryComponent, targetVersion);
            return;
        }

        boolean isStagedDeploy = stageWithJobs.size() > 1;
        List<ExecutionNodeEntity> executionJobList = new ArrayList<>();
        Integer flowParallelism = flowEntity.getParallelism();
        // 是否为多阶段类型发布
        int batchId = 0;
        int curs;

        for (Map.Entry<String, Set<String>> entry : stageWithJobs.entrySet()) {
            String stage = entry.getKey();
            // insert stage start logical node
            if (isStagedDeploy) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(stage);
                jobEntity.setFlowId(flowId);
                String id = UUID.randomUUID().toString().replace(Constants.BAR, Constants.EMPTY_STRING);
                jobEntity.setNodeName(id);
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                jobEntity.setBatchId(++batchId);
                jobEntity.setNodeType(NodeType.STAGE_START_NODE);
                executionJobList.add(jobEntity);
            }

            ++batchId;
            curs = 0;
            Set<String> stageJobList = entry.getValue();
            Preconditions.checkState(!CollectionUtils.isEmpty(stageJobList), "stageJobList is null, stage is: " + stage);
            for (String jobId : stageJobList) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(stage);
                jobEntity.setFlowId(flowId);
                jobEntity.setNodeName(jobId);
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                jobEntity.setBatchId(batchId);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                executionJobList.add(jobEntity);
                if (++curs >= flowParallelism) {
                    curs = 0;
                    batchId++;
                }
            }

            // insert stage end logical node
            if (isStagedDeploy) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(stage);
                jobEntity.setFlowId(flowId);
                String id = UUID.randomUUID().toString().replace(Constants.BAR, Constants.EMPTY_STRING);
                jobEntity.setNodeName(id);
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                if (curs == 0) {
                    jobEntity.setBatchId(batchId);
                } else {
                    jobEntity.setBatchId(++batchId);
                }
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                jobEntity.setNodeType(NodeType.STAGE_END_NODE);
                executionJobList.add(jobEntity);
            }
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionJobList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("save flow {} execution node list success.", flowId);

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("spark periphery component {} deploy event list is {}", peripheryComponent, resolvedEventList);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(
                FlowDeployType.SPARK_PERIPHERY_COMPONENT_DEPLOY,
                FlowDeployType.SPARK_PERIPHERY_COMPONENT_ROLLBACK,
                FlowDeployType.SPARK_PERIPHERY_COMPONENT_LOCK,
                FlowDeployType.SPARK_PERIPHERY_COMPONENT_RELEASE);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.SPARK_PERIPHERY_COMPONENT_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
