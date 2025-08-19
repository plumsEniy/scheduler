package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowGroupTypeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommonDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    protected ExecutionFlowService flowService;

    @Resource
    protected ExecutionNodeService nodeService;

    @Resource
    protected ExecutionNodeEventService nodeEventService;

    @Resource
    protected GlobalService globalService;

    @Resource
    protected BmrResourceService bmrResourceService;

    @Resource
    protected BmrMetadataService bmrMetadataService;

    @Resource
    protected ExecutionFlowPropsService flowPropsService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();

        Long clusterId = flowEntity.getClusterId();
        MetadataClusterData clusterDetail = bmrMetadataService.queryClusterDetail(clusterId);
        Preconditions.checkNotNull(clusterDetail, "集群信息为空");

        Long componentId = flowEntity.getComponentId();
        MetadataComponentData componentDetail = bmrMetadataService.queryComponentByComponentId(componentId);
        Preconditions.checkNotNull(componentDetail, "组件信息为空");
        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, clusterDetail, componentDetail);

        BaseFlowExtPropDTO flowExtPropDTO = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);

        List<String> nodeList;
        String releaseScopeType = flowEntity.getReleaseScopeType();
        if (StringUtils.isBlank(releaseScopeType)) {
            nodeList = flowExtPropDTO.getNodeList();
        } else {
            FlowReleaseScopeType flowReleaseScopeType = FlowReleaseScopeType.valueOf(releaseScopeType);
//            switch (flowReleaseScopeType) {
//                case FULL_RELEASE:
//                    globalService.getBmrResourceService().queryAllNodes();
//            }
            nodeList = flowExtPropDTO.getNodeList();
        }

        Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "发布节点列表为空");
        List<ExecutionNodeEntity> executionNodeList = getNodeEntityList(flowEntity, nodeList, 1, 0);
//        Integer maxBatchId = executionNodeList.get(executionNodeList.size() - 1).getBatchId();
//        flowService.updateMaxBatchId(flowId, maxBatchId);

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("save flow {} execution node list success.", flowId);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    protected List<ExecutionNodeEntity> getNodeEntityList(ExecutionFlowEntity flowEntity, List<String> nodeList, Integer startBatchId, int flowParallelism) {
        List<ResourceHostInfo> hostInfoList = bmrResourceService.queryHostListByName(nodeList);
        Map<String, ResourceHostInfo> hostNameToHostInfoMap = hostInfoList.stream().collect(
                Collectors.toMap(ResourceHostInfo::getHostName, Function.identity()));

        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        if (flowParallelism <= 0) {
            flowParallelism = flowEntity.getParallelism();
        }
        int batchId = startBatchId;
        int curs = 0;
        // 随机划分和按机架划分
        FlowGroupTypeEnum groupType = flowEntity.getGroupType();
        switch (groupType) {
            case RANDOM_GROUP:
                for (String node : nodeList) {
                    ResourceHostInfo hostInfo = hostNameToHostInfoMap.get(node);
                    String rack = Constants.EMPTY_STRING;
                    String ip = Constants.EMPTY_STRING;
                    if (!Objects.isNull(hostInfo)) {
                        rack = hostInfo.getRack();
                        ip = hostInfo.getIp();
                    }
                    ExecutionNodeEntity executionNode = new ExecutionNodeEntity();
                    executionNode.setNodeName(node);
                    executionNode.setOperator(flowEntity.getOperator());
                    executionNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                    executionNode.setBatchId(batchId);
                    executionNode.setFlowId(flowEntity.getId());
                    executionNode.setOperationResult(NodeOperationResult.NORMAL);
                    executionNode.setRack(rack);
                    executionNode.setIp(ip);
                    executionNodeList.add(executionNode);
                    if (++curs >= flowParallelism) {
                        curs = 0;
                        batchId++;
                    }
                }
                break;
            case RACK_GROUP:
                Map<String, List<ResourceHostInfo>> rackToComponentNodeDetailMap = new HashMap<>();
                nodeList.forEach(node -> {
                    ResourceHostInfo hostInfo = hostNameToHostInfoMap.get(node);
                    Assert.isTrue(!Objects.isNull(hostInfo), node + "主机无法找到信息");
                    String rack = hostInfo.getRack();
                    Assert.isTrue(!StringUtils.isEmpty(rack) && !Constants.NAN.equalsIgnoreCase(rack), node + "主机无法找到基架信息");
                    rackToComponentNodeDetailMap.computeIfAbsent(rack, v -> new ArrayList<>()).add(hostInfo);
                });

                //                每个机架内根据并发度拆分
                for (Map.Entry<String, List<ResourceHostInfo>> entry : rackToComponentNodeDetailMap.entrySet()) {
                    String rack = entry.getKey();
                    List<ResourceHostInfo> rackToHostInfoList = entry.getValue();
                    curs = 0;
                    for (ResourceHostInfo componentNodeDetail : rackToHostInfoList) {
                        String hostname = componentNodeDetail.getHostName();
                        ResourceHostInfo hostInfo = hostNameToHostInfoMap.get(hostname);
                        ExecutionNodeEntity executionNode = new ExecutionNodeEntity();
                        executionNode.setNodeName(hostname);
                        executionNode.setOperator(flowEntity.getOperator());
                        executionNode.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                        executionNode.setBatchId(batchId);
                        executionNode.setRack(rack);
                        executionNode.setFlowId(flowEntity.getId());
                        executionNode.setOperationResult(NodeOperationResult.NORMAL);
                        executionNode.setRack(rack);
                        executionNode.setIp(hostInfo.getIp());
                        executionNodeList.add(executionNode);
                        if (++curs >= flowParallelism) {
                            curs = 0;
                            batchId++;
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("cannot handler group type: " + groupType);
        }
        return executionNodeList;
    }


    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        final MetadataComponentData componentDetail = pipelineParameter.getComponentData();
        Preconditions.checkNotNull(componentDetail, "componentDetail is not exist");
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(componentDetail.getComponentName(), PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
