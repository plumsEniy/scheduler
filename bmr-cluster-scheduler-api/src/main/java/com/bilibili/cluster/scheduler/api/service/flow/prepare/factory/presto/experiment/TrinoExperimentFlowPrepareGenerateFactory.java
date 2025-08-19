package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto.experiment;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobProps;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentJobType;
import com.bilibili.cluster.scheduler.common.dto.bmr.experiment.ExperimentType;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoClusterInfo;
import com.bilibili.cluster.scheduler.common.dto.presto.experiment.TrinoExperimentExtFlowParams;
import com.bilibili.cluster.scheduler.common.dto.presto.iteration.PrestoIterationExtNodeParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TrinoExperimentFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    BmrMetadataService metadataService;

    /**
     * 两阶段任务
     * @param flowEntity
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        try {
            final Long flowId = flowEntity.getId();
            BaseFlowExtPropDTO flowProps = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
            String flowExtParams = flowProps.getFlowExtParams();
            TrinoExperimentExtFlowParams extFlowParams = JSONUtil.toBean(flowExtParams, TrinoExperimentExtFlowParams.class);
            final List<String> jobList = flowProps.getNodeList();

            final String aRunTimeConf = extFlowParams.getARunTimeConf();
            final TrinoClusterInfo aClusterInfo = JSONUtil.toBean(aRunTimeConf, TrinoClusterInfo.class);
            final MetadataClusterData aClusterData = metadataService.queryClusterDetail(aClusterInfo.getClusterId());
            Preconditions.checkState(aClusterData.getClusterEnvironment().equalsIgnoreCase("TEST"),
                    "集群环境需要是测试集群: " + aClusterData.getClusterName());
            // 是否重建A集群
            boolean requireRebuildACluster = aClusterInfo.isRebuildCluster();

            TrinoClusterInfo bClusterInfo = null;
            MetadataClusterData bClusterData = null;
            // 是否重建B集群
            boolean requireRebuildBCluster = false;
            if (extFlowParams.getExperimentType().equals(ExperimentType.COMPARATIVE_TASK)) {
                final String bRunTimeConf = extFlowParams.getBRunTimeConf();
                bClusterInfo = JSONUtil.toBean(bRunTimeConf, TrinoClusterInfo.class);
                bClusterData = metadataService.queryClusterDetail(bClusterInfo.getClusterId());
                Preconditions.checkState(bClusterData.getClusterEnvironment().equalsIgnoreCase("TEST"),
                        "集群环境需要是测试集群: " + bClusterData.getClusterName());
                requireRebuildBCluster = bClusterInfo.isRebuildCluster();
            }

            List<ExecutionNodeEntity> executionJobList = new ArrayList<>();
            Integer flowParallelism = flowEntity.getParallelism();
            // 是否为多阶段类型发布
            int batchId = 0;
            int curs = 0;

            // stage-1 node props
            Map<String, PrestoIterationExtNodeParams> clusterExtInfoMap = new HashMap<>();
            // stage-2 node props
            Map<String, ExperimentJobProps> experimentJobExtInfoMap = new HashMap<>();

            if (requireRebuildACluster || requireRebuildBCluster) {
                // stage-1 start node
                final ExecutionNodeEntity stage1StartJob = new ExecutionNodeEntity();
                stage1StartJob.setExecStage("1");
                stage1StartJob.setFlowId(flowId);
                stage1StartJob.setNodeName("Trino集群迭代发布-开始阶段");
                stage1StartJob.setOperator(flowEntity.getOperator());
                stage1StartJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                stage1StartJob.setOperationResult(NodeOperationResult.NORMAL);
                stage1StartJob.setBatchId(++batchId);
                stage1StartJob.setNodeType(NodeType.STAGE_START_NODE);
                executionJobList.add(stage1StartJob);

                ++batchId;
                if (requireRebuildACluster) {
                    //  a集群迭代发布
                    final ExecutionNodeEntity prestoAIterationJob = new ExecutionNodeEntity();
                    prestoAIterationJob.setExecStage("1");
                    prestoAIterationJob.setFlowId(flowId);
                    String aClusterName = aClusterData.getClusterName();
                    prestoAIterationJob.setNodeName(aClusterName);
                    prestoAIterationJob.setOperator(flowEntity.getOperator());
                    prestoAIterationJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    prestoAIterationJob.setOperationResult(NodeOperationResult.NORMAL);
                    prestoAIterationJob.setBatchId(batchId);
                    prestoAIterationJob.setNodeType(NodeType.NORMAL);
                    executionJobList.add(prestoAIterationJob);
                    // presto迭代所需的额外信息
                    final PrestoIterationExtNodeParams nodeParams = new PrestoIterationExtNodeParams();
                    nodeParams.setClusterId(aClusterInfo.getClusterId());
                    nodeParams.setComponentId(aClusterInfo.getComponentId());
                    nodeParams.setPackId(aClusterInfo.getPackId());
                    nodeParams.setConfigId(aClusterInfo.getConfigId());
                    clusterExtInfoMap.put(aClusterName, nodeParams);
                }

                if (requireRebuildBCluster) {
                    //  b集群迭代发布
                    final ExecutionNodeEntity prestoBIterationJob = new ExecutionNodeEntity();
                    prestoBIterationJob.setExecStage("1");
                    prestoBIterationJob.setFlowId(flowId);
                    String bClusterName = bClusterData.getClusterName();
                    prestoBIterationJob.setNodeName(bClusterName);
                    prestoBIterationJob.setOperator(flowEntity.getOperator());
                    prestoBIterationJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    prestoBIterationJob.setOperationResult(NodeOperationResult.NORMAL);
                    prestoBIterationJob.setBatchId(batchId);
                    prestoBIterationJob.setNodeType(NodeType.NORMAL);
                    executionJobList.add(prestoBIterationJob);
                    // presto迭代所需的额外信息
                    final PrestoIterationExtNodeParams nodeParams = new PrestoIterationExtNodeParams();
                    nodeParams.setClusterId(bClusterInfo.getClusterId());
                    nodeParams.setComponentId(bClusterInfo.getComponentId());
                    nodeParams.setPackId(bClusterInfo.getPackId());
                    nodeParams.setConfigId(bClusterInfo.getConfigId());
                    clusterExtInfoMap.put(bClusterName, nodeParams);
                }

                // stage-1 end node
                final ExecutionNodeEntity stage1EndJob = new ExecutionNodeEntity();
                stage1EndJob.setExecStage("1");
                stage1EndJob.setFlowId(flowId);
                stage1EndJob.setNodeName("Trino集群迭代发布-结束阶段");
                stage1EndJob.setOperator(flowEntity.getOperator());
                stage1EndJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                stage1EndJob.setOperationResult(NodeOperationResult.NORMAL);
                stage1EndJob.setBatchId(++batchId);
                stage1EndJob.setNodeType(NodeType.STAGE_END_NODE);
                executionJobList.add(stage1EndJob);

                // stage-2 experiment start job
                final ExecutionNodeEntity stage2StartJob = new ExecutionNodeEntity();
                stage2StartJob.setExecStage("2");
                stage2StartJob.setFlowId(flowId);
                stage2StartJob.setNodeName("Trino实验任务-开始阶段");
                stage2StartJob.setOperator(flowEntity.getOperator());
                stage2StartJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                stage2StartJob.setOperationResult(NodeOperationResult.NORMAL);
                stage2StartJob.setBatchId(++batchId);
                stage2StartJob.setNodeType(NodeType.STAGE_START_NODE);
                executionJobList.add(stage2StartJob);

                ++batchId;
                for (String jobId : jobList) {
                    final ExecutionNodeEntity stage2ExperimentJob = new ExecutionNodeEntity();
                    stage2ExperimentJob.setExecStage("2");
                    stage2ExperimentJob.setFlowId(flowId);
                    stage2ExperimentJob.setNodeName(jobId);
                    stage2ExperimentJob.setOperator(flowEntity.getOperator());
                    stage2ExperimentJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    stage2ExperimentJob.setOperationResult(NodeOperationResult.NORMAL);
                    stage2ExperimentJob.setBatchId(batchId);
                    stage2ExperimentJob.setNodeType(NodeType.NORMAL);
                    executionJobList.add(stage2ExperimentJob);

                    if (++curs >= flowParallelism) {
                        curs = 0;
                        batchId++;
                    }
                    // trino实验所需额外参数
                    ExperimentJobProps jobProps = generateJobProps(jobId, extFlowParams);
                    jobProps.setOpUser(flowEntity.getOperator());
                    experimentJobExtInfoMap.put(jobId, jobProps);
                }
                // stage-2 experiment end job
                final ExecutionNodeEntity stage2EndJob = new ExecutionNodeEntity();
                stage2EndJob.setExecStage("2");
                stage2EndJob.setFlowId(flowId);
                stage2EndJob.setNodeName("Trino实验任务-结束阶段");
                stage2EndJob.setOperator(flowEntity.getOperator());
                stage2EndJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                stage2EndJob.setOperationResult(NodeOperationResult.NORMAL);
                stage2EndJob.setBatchId(++batchId);
                stage2EndJob.setNodeType(NodeType.STAGE_END_NODE);
                executionJobList.add(stage2StartJob);
            } else {
                ++batchId;
                // 不用区分stage
                for (String jobId : jobList) {
                    final ExecutionNodeEntity experimentJob = new ExecutionNodeEntity();
                    experimentJob.setExecStage("2");
                    experimentJob.setFlowId(flowId);
                    experimentJob.setNodeName(jobId);
                    experimentJob.setOperator(flowEntity.getOperator());
                    experimentJob.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    experimentJob.setOperationResult(NodeOperationResult.NORMAL);
                    experimentJob.setBatchId(batchId);
                    experimentJob.setNodeType(NodeType.NORMAL);
                    executionJobList.add(experimentJob);

                    if (++curs >= flowParallelism) {
                        curs = 0;
                        batchId++;
                    }
                    // trino实验所需额外参数
                    ExperimentJobProps jobProps = generateJobProps(jobId, extFlowParams);
                    jobProps.setOpUser(flowEntity.getOperator());
                    experimentJobExtInfoMap.put(jobId, jobProps);
                }
            }

            List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionJobList, 500);
            for (List<ExecutionNodeEntity> split : splitList) {
                Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
            }
            log.info("save flow {} execution node list success.", flowId);

            for (ExecutionNodeEntity nodeEntity : executionJobList) {
                final String jobId = nodeEntity.getNodeName();
                // 跳过逻辑节点
                if (!nodeEntity.getNodeType().isNormalNode()) {
                    continue;
                }

                Object nodeProps = null;
                final String execStage = nodeEntity.getExecStage();
                String execNodeName = nodeEntity.getNodeName();

                if (execStage.equals("1")) {
                    // 阶段一对trino集群做迭代
                    PrestoIterationExtNodeParams nodeParams = clusterExtInfoMap.get(execNodeName);
                    Preconditions.checkNotNull(nodeParams,
                            "stage 1, but find prestoIterationExtNodeParams is null, cluster name is " + execNodeName);
                    nodeParams.setNodeId(nodeEntity.getId());
                    nodeProps = nodeParams;
                } else {
                    // 阶段2执行trino实验任务
                    final ExperimentJobProps jobProps = experimentJobExtInfoMap.get(execNodeName);
                    Preconditions.checkNotNull(jobProps,
                            "stage 2, but find experimentJobProps is null， jobId is " + execNodeName);
                    jobProps.setNodeId(nodeEntity.getId());
                    nodeProps = jobProps;
                }

                Preconditions.checkNotNull(nodeProps, "nodeProps is null, exec node name is " + execNodeName);
                nodePropsService.saveNodeProp(nodeEntity.getId(), nodeProps);
            }
            log.info("save flow {} execution job props success.", flowId);

            List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
            log.info("TrinoExperimentFlowPrepareGenerateFactory#resolvedEventList is {}", resolvedEventList);

            nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
            log.info("save flow {} execution job event success.", flowId);
        } catch (Exception e) {
            log.error("TrinoExperimentFlowPrepareGenerateFactory#generateNodeAndEvents failed: {}", e.getMessage());
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private ExperimentJobProps generateJobProps(String jobId, TrinoExperimentExtFlowParams flowExtParams) {
        final ExperimentJobProps jobProps = new ExperimentJobProps();
        jobProps.setJobId(jobId);
        // 仅支持手动录入的测试任务
        jobProps.setJobType(ExperimentJobType.TEST_JOB);
        jobProps.setTestSetVersionId(flowExtParams.getTestSetVersionId());

        ExperimentType experimentType = flowExtParams.getExperimentType();
        jobProps.setExperimentType(flowExtParams.getExperimentType());

        jobProps.setCiInstanceId(flowExtParams.getInstanceId());

        String platformA = flowExtParams.getPlatformA();
        jobProps.setPlatformA(platformA);
        final String confA = flowExtParams.getConfA();
        jobProps.setConfA(confA);

        switch (experimentType) {
            case PERFORMANCE_TEST:
                jobProps.setPlatformB(Constants.EXPERIMENT_PLATFORM_EMPTY_VALUE);
                jobProps.setMetrics("CPU,MEMORY,DURATION");
                break;
            case COMPARATIVE_TASK:
                String platformB = flowExtParams.getPlatformB();
                jobProps.setPlatformB(platformB);
                jobProps.setMetrics("COUNT,CRC32,CPU,MEMORY,DURATION");
                final String confB = flowExtParams.getConfB();
                jobProps.setConfB(confB);
                break;
        }
        return jobProps;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.TRINO_EXPERIMENT);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.TRINO_EXPERIMENT_PIPELINE_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
