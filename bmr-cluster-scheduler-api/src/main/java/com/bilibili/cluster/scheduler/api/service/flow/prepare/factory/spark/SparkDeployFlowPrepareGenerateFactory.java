package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.spark;

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
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.redis.RedisService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.InstallationPackage;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobInfoDTO;
import com.bilibili.cluster.scheduler.common.dto.spark.manager.SparkJobLabel;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployType;
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
import org.apache.flink.annotation.Public;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SparkDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    SparkManagerService sparkManagerService;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    RedisService redisService;

    private final static Pattern versionPattern = Pattern.compile("v(\\d+)\\..*");


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();
        BaseFlowExtPropDTO flowProps = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        String flowExtParamsValue = flowProps.getFlowExtParams();

        String cacheKey = CacheUtils.getFlowExtParamsCacheKey(SpringApplicationContext.getEnv(), flowId);
        redisService.set(cacheKey, flowExtParamsValue, Constants.ONE_MINUTES * 60);

        SparkDeployFlowExtParams flowExtParams = JSONUtil.toBean(flowExtParamsValue, SparkDeployFlowExtParams.class);
        final FlowDeployType deployType = flowEntity.getDeployType();
        final String releaseScopeTypeValue = flowEntity.getReleaseScopeType();
        FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);

        List<String> jobIdList = flowProps.getNodeList();
        String targetSparkVersion = flowExtParams.getTargetSparkVersion();
        log.info("spark version deploy target spark version is {}", targetSparkVersion);
        final String majorSparkVersion = flowExtParams.getMajorSparkVersion();

        final SparkDeployType sparkDeployType = flowExtParams.getSparkDeployType();
        Map<String, Set<String>> stageWithJobs;
        switch (deployType) {
            case SPARK_DEPLOY:
                stageWithJobs = buildSparkDeployStageToMap(flowEntity, flowExtParams, jobIdList);
                break;
            case SPARK_DEPLOY_ROLLBACK:
                stageWithJobs = buildSparkRollbackStageToMap(flowId, flowExtParams, deployType, sparkDeployType);
                if (stageWithJobs == null) return;
                break;
            default:
                throw new IllegalArgumentException("unsupported deploy type");
        }

        boolean isStagedDeploy = false;
        if (deployType == FlowDeployType.SPARK_DEPLOY && sparkDeployType == SparkDeployType.NORMAL && (stageWithJobs.size() > 1)) {
            isStagedDeploy = true;
        }

        if (FlowReleaseScopeType.FULL_RELEASE.equals(flowReleaseScopeType)) {
            InstallationPackage sparkDefaultPackage = bmrMetadataService.querySparkDefaultPackage();
            if (!Objects.isNull(sparkDefaultPackage)) {
                flowExtParams.setOriginalSparkVersion(sparkDefaultPackage.getMinorVersion());
                flowProps.setFlowExtParams(JSONUtil.toJsonStr(flowExtParams));
                executionFlowPropsService.saveFlowProp(flowId, flowProps);
            }
        }

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
        log.info("spark version deploy event list is {}", resolvedEventList);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    @Nullable
    private Map<String, Set<String>> buildSparkRollbackStageToMap(Long flowId, SparkDeployFlowExtParams flowExtParams, FlowDeployType deployType, SparkDeployType sparkDeployType) {
        List<String> jobIdList;
        Map<String, Set<String>> stageWithJobs;
        final String originalSparkVersion = flowExtParams.getOriginalSparkVersion();
        jobIdList = sparkManagerService.queryTargetVersionJobList(originalSparkVersion);
        if (CollectionUtils.isEmpty(jobIdList)) {
            log.info("spark version deploy rollback, jobIdList is empty, skipped. flow id is {}", flowId);
            return null;
        }
        stageWithJobs = new HashMap<>();
        List<Integer> percentStageList = flowExtParams.getPercentStageList();

        if (deployType == FlowDeployType.SPARK_DEPLOY && sparkDeployType == SparkDeployType.NORMAL) {
            stageWithJobs = StageSplitUtil.buildStageMap(jobIdList, percentStageList);
        } else {
            stageWithJobs.put("", new HashSet<>(jobIdList));
        }
        return stageWithJobs;
    }

    private Map<String, Set<String>> buildSparkDeployStageToMap(ExecutionFlowEntity flowEntity, SparkDeployFlowExtParams flowExtParams, List<String> jobIdList) {
        final String releaseScopeTypeValue = flowEntity.getReleaseScopeType();
        FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeTypeValue);
        String targetSparkVersion = flowExtParams.getTargetSparkVersion();
        String version = getVersion(targetSparkVersion);
        final SparkDeployType sparkDeployType = flowExtParams.getSparkDeployType();

        Map<String, Set<String>> stageWithJobs = new HashMap<>();
        switch (version) {
            case "3":
                if (flowReleaseScopeType == FlowReleaseScopeType.FULL_RELEASE) {
                    jobIdList = sparkManagerService.queryAllPublishJobExcludeByVersion(targetSparkVersion)
                            .stream().map(SparkJobInfoDTO::getJobId).collect(Collectors.toList());
                }

                stageWithJobs = handleSparkDeployType(flowExtParams, jobIdList, sparkDeployType);
                break;
            case "4":
                switch (flowReleaseScopeType) {
                    case FULL_RELEASE:
                        stageWithJobs = handleSpark4FullReleaseDeployType(targetSparkVersion, sparkDeployType);
                        break;
                    case GRAY_RELEASE:
                        stageWithJobs = handleSparkDeployType(flowExtParams, jobIdList, sparkDeployType);
                        break;
                }
                break;
            default:
                throw new IllegalArgumentException("spark version is not support, version is " + version);
        }
        return stageWithJobs;
    }

    private Map<String, Set<String>> handleSpark4FullReleaseDeployType(String majorSparkVersion, SparkDeployType sparkDeployType) {
        Map<String, Set<String>> stageWithJobs = new LinkedHashMap<>();

        // 查询指定Spark主版本号的全量发布任务列表
        List<SparkJobInfoDTO> fullSparkJobInfoList = sparkManagerService.queryAllPublishJobExcludeByVersion(majorSparkVersion);
        switch (sparkDeployType) {
            case NORMAL:
                // 将任务列表按标签分组
                Map<SparkJobLabel, List<SparkJobInfoDTO>> labelToSparkJobInfoMap = fullSparkJobInfoList.stream()
                        .collect(Collectors.groupingBy(SparkJobInfoDTO::getLabel));
                // 获取所有阶段列表，并按阶段将任务ID分组(会排除掉default)
                List<SparkJobLabel> stageList = SparkJobLabel.getStageList();
                for (SparkJobLabel jobLabel : stageList) {
                    List<SparkJobInfoDTO> stageJobList = labelToSparkJobInfoMap.getOrDefault(jobLabel, Collections.emptyList());
                    stageWithJobs.put(String.valueOf(jobLabel.getOrder()), stageJobList.stream().map(SparkJobInfoDTO::getJobId).collect(Collectors.toSet()));
                }
                break;
            case EMERGENCY:
                stageWithJobs.put("", fullSparkJobInfoList.stream().map(SparkJobInfoDTO::getJobId).collect(Collectors.toSet()));
                break;
        }
        return stageWithJobs;
    }

    private static Map<String, Set<String>> handleSparkDeployType(SparkDeployFlowExtParams flowExtParams, List<String> jobIdList, SparkDeployType sparkDeployType) {
        Map<String, Set<String>> stageWithJobs;
        switch (sparkDeployType) {
            case NORMAL:
                // 正常部署模式下，根据阶段百分比列表构建阶段与任务ID的映射
                stageWithJobs = StageSplitUtil.buildStageMap(jobIdList, flowExtParams.getPercentStageList());
                break;
            case EMERGENCY:
                // 紧急部署模式下，将所有任务ID放入一个空阶段中
                stageWithJobs = new HashMap<>();
                stageWithJobs.put("", new HashSet<>(jobIdList));
                break;
            default:
                throw new IllegalArgumentException("spark deploy type is not valid, spark deploy type is " + sparkDeployType);
        }
        return stageWithJobs;
    }

    private static String getVersion(String majorSparkVersion) {
        Matcher matcher = versionPattern.matcher(majorSparkVersion);
        if (matcher.find()) {
            String majorVersion = matcher.group(1);
            return majorVersion;
        }
        throw new IllegalArgumentException("spark version is not valid");
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.SPARK_DEPLOY, FlowDeployType.SPARK_DEPLOY_ROLLBACK);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.SPARK_DEPLOY_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
