package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.clickhouse;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.clickhouse.clickhouse.ClickhouseService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.CommonDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.ClickhouseDeployDTO;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkContainerCapacityFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.params.CkContainerIterationFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.ClickhouseCluster;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.Replica;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.Shards;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.clickhouse.CKClusterType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 包含ck的容器扩容和缩容
 * @Date: 2025/2/11 17:18
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class ClickHouseContainerPrepareGenerateFactory extends CommonDeployFlowPrepareGenerateFactory {

    @Resource
    ClickhouseService clickhouseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        final BaseFlowExtPropDTO baseFlowExtPropDTO = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        FlowDeployType deployType = flowEntity.getDeployType();
        String podTemplate;
        String flowExtParamStr = baseFlowExtPropDTO.getFlowExtParams();
        ClickhouseDeployDTO clickhouseDeployDTO;
        Long configId = flowEntity.getConfigId();

        switch (deployType) {
            case K8S_CAPACITY_EXPANSION:
                CkContainerCapacityFlowExtParams ckContainerCapacityFlowExtParams = JSONUtil.toBean(flowExtParamStr, CkContainerCapacityFlowExtParams.class);
                List<Integer> shardAllocationList = ckContainerCapacityFlowExtParams.getShardAllocationList();
                podTemplate = ckContainerCapacityFlowExtParams.getPodTemplate();
                clickhouseDeployDTO = clickhouseService.buildScaleDeployDTO(configId, podTemplate, shardAllocationList);
                break;
            case K8S_ITERATION_RELEASE:
                CkContainerIterationFlowExtParams ckContainerIterationFlowExtParams = JSONUtil.toBean(flowExtParamStr, CkContainerIterationFlowExtParams.class);
                podTemplate = ckContainerIterationFlowExtParams.getPodTemplate();
                List<String> iterationPodList = ckContainerIterationFlowExtParams.getIterationPodList();
                clickhouseDeployDTO = clickhouseService.buildIterationDeployDTO(configId, podTemplate, iterationPodList);
                break;
            default:
                throw new IllegalArgumentException("不支持的部署类型" + deployType);
        }

        List<ClickhouseCluster> clusterList = clickhouseDeployDTO.getChConfig().getClusters();
        ClickhouseCluster adminCluster = clusterList.stream().filter(cluster -> cluster.getClusterType().equals(CKClusterType.ADMIN))
                .findAny().get();
//        admin中shard 只有1个副本
        List<Shards> adminShardList = adminCluster.getLayout().getShards();

        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        int batchId = 1;
        String[] appidSplit = clickhouseDeployDTO.getAppId().split("\\.");
        String chiName = appidSplit[appidSplit.length - 1];

        for (Shards shard : adminShardList) {
            for (Replica replica : shard.getReplicas()) {
                ExecutionNodeEntity executionNode = new ExecutionNodeEntity();
                executionNode.setNodeName("chi-" + chiName + Constants.LINE + replica.getName() + Constants.LINE + "0");
                executionNode.setOperator(flowEntity.getOperator());
                executionNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                executionNode.setBatchId(batchId);
                executionNode.setFlowId(flowEntity.getId());
                executionNode.setOperationResult(NodeOperationResult.NORMAL);
                executionNode.setRack(Constants.EMPTY_STRING);
                executionNode.setIp(Constants.EMPTY_STRING);
                executionNodeList.add(executionNode);
            }
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }

        log.info("save flow {} execution node list success.", flowId);
        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.CK_CONTAINER_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }
}
