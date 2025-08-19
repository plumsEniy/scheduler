package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.config.BmrConfigService;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.bmr.resourceV2.BmrResourceV2Service;
import com.bilibili.cluster.scheduler.api.service.flow.*;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.model.ResourceHostInfo;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.NNProxyPriority;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.ComponentConfInfo;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms.NNProxyDeployNodePreFillExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.*;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.bilibili.cluster.scheduler.common.enums.node.NodeType;
import com.bilibili.cluster.scheduler.common.utils.NumberUtils;
import com.bilibili.cluster.scheduler.common.utils.StageSplitUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NNProxyDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    BmrConfigService bmrConfigService;

    @Resource
    BmrResourceV2Service resourceV2Service;

    @Resource
    BmrMetadataService bmrMetadataService;

    @Resource
    BmrResourceService bmrResourceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        BaseFlowExtPropDTO flowProps = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        final List<String> nodeList = flowProps.getNodeList();

        NNProxyDeployFlowExtParams flowExtParams = flowPropsService.getFlowExtParamsByCache(flowId, NNProxyDeployFlowExtParams.class);
        final SubDeployType subDeployType = flowExtParams.getSubDeployType();

        // 按发布类型拆分
        switch (subDeployType) {
            case CAPACITY_EXPANSION:
                generateExpansionNodeAndEvents(nodeList, flowEntity);
                break;
            case ITERATION_RELEASE:
                generateIterationNodeAndEvents(nodeList, flowEntity, flowExtParams);
                break;
            default:
                throw new IllegalArgumentException("un-support NNProxy deploy type: " + subDeployType);
        }
    }

    private void generateExpansionNodeAndEvents(List<String> nodeList, ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        List<ResourceHostInfo> hostInfoList = bmrResourceService.queryHostListByName(nodeList);
        Map<String, ResourceHostInfo> hostNameToHostInfoMap = hostInfoList.stream().collect(
                Collectors.toMap(ResourceHostInfo::getHostName, Function.identity()));

        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        Integer flowParallelism = flowEntity.getParallelism();
        int batchId = 1;
        int curs = 0;

        for (String node : nodeList) {
            ResourceHostInfo hostInfo = hostNameToHostInfoMap.get(node);
            String rack = Constants.EMPTY_STRING;
            if (!StringUtils.isEmpty(hostInfo.getRack())) {
                rack = hostInfo.getRack();
            }
            String ip = hostInfo.getIp();
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

        final PipelineParameter pipelineParameter = new PipelineParameter();
        pipelineParameter.setFlowEntity(flowEntity);
        final List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(pipelineParameter);

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("{}: save flow {} execution node list success.", getName(), flowId);

        for (ExecutionNodeEntity nodeEntity : executionNodeList) {
            final NNProxyDeployNodePreFillExtParams nodeProps = new NNProxyDeployNodePreFillExtParams();
            final Long nodeId = nodeEntity.getId();
            Preconditions.checkState(NumberUtils.isPositiveLong(nodeId), "node id not exists: " + JSONUtil.toJsonStr(nodeEntity));
            nodeProps.setNodeId(nodeId);
            nodeProps.setContainPackage(true);
            nodeProps.setPackageId(flowEntity.getPackageId());
            nodeProps.setContainConfig(true);
            nodeProps.setConfigId(flowEntity.getConfigId());
            nodeProps.setComponentId(flowEntity.getComponentId());
            nodePropsService.saveNodeProp(nodeId, nodeProps);
        }
        log.info("{}: save flow {} execution node list props success.", getName(), flowId);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("{}: save flow {} execution job event success.", getName(), flowId);
    }

    private void generateIterationNodeAndEvents(List<String> nodeList, ExecutionFlowEntity flowEntity, NNProxyDeployFlowExtParams flowExtParams) throws Exception {
        final String releaseScopeType = flowEntity.getReleaseScopeType();
        final FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);
        final List<Integer> percentList = Arrays.asList(10, 50, 100);
        final Long clusterId = flowEntity.getClusterId();
        final List<ComponentConfInfo> confInfoList = flowExtParams.getConfInfoList();

        if (!CollectionUtils.isEmpty(confInfoList)) {
            List<Long> componentIdList = confInfoList.stream().map(ComponentConfInfo::getComponentId)
                    .collect(Collectors.toList());
            for (Long componentId : componentIdList) {
                QueryComponentNodeListReq queryComponentNodeListReq = new QueryComponentNodeListReq();
                queryComponentNodeListReq.setClusterId(clusterId);
                queryComponentNodeListReq.setComponentId(componentId);
                queryComponentNodeListReq.setPageNum(1);
                queryComponentNodeListReq.setPageSize((long) Constants.PAGE_MAX);
                queryComponentNodeListReq.setNeedDns(true);
                List<ComponentNodeDetail> componentNodeList = bmrResourceService.queryNodeList(queryComponentNodeListReq);
                componentNodeList.forEach(node -> {
                    String dns = node.getDns();
                    if (StringUtils.isBlank(dns)) {
                        String errorMsg = String.format("该节点的dns为空,集群id%s,组件id%s,节点%s", clusterId, componentId, node.getHostName());
                        throw new RuntimeException(errorMsg);
                    }
                    if (!Constants.DNS_PATTERN.matcher(dns).matches()) {
                        String errorMsg = String.format("%s的dns格式不正确,%s", node.getHostName(), dns);
                        throw new RuntimeException(errorMsg);
                    }
                });
            }
        }

        final Map<String, Set<String>> stageWithNodes;
        final Map<String, Long> hostNameToConfigId = new HashMap<>();
        final Map<String, Long> hostNameToComponentId = new HashMap<>();

        FlowDeployPackageType deployPackageType = FlowDeployPackageType.valueOf(flowEntity.getDeployPackageType());
        boolean isContainConfigDeploy = deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                deployPackageType == FlowDeployPackageType.CONFIG_PACKAGE;
        boolean isContainPackageDeploy = deployPackageType == FlowDeployPackageType.ALL_PACKAGE ||
                deployPackageType == FlowDeployPackageType.SERVICE_PACKAGE;

        switch (scopeType) {
            case GRAY_RELEASE:
                stageWithNodes = buildGrayStageMap(nodeList, percentList, flowExtParams.getUrgencyType());
                if (isContainConfigDeploy) {
                    nodeList.forEach(host -> hostNameToConfigId.put(host, flowEntity.getConfigId()));
                }
                nodeList.forEach(host -> hostNameToComponentId.put(host, flowEntity.getComponentId()));
                break;
            case FULL_RELEASE:
                final QueryComponentNodeListReq req = new QueryComponentNodeListReq(flowEntity.getClusterId(), null, null);
                final List<ComponentNodeDetail> allNodeDetailList = bmrResourceService.queryNodeList(req);
                Preconditions.checkState(!CollectionUtils.isEmpty(allNodeDetailList), "allNodeDetailList is empty, cluster id is: " + flowEntity.getClusterId());
                stageWithNodes = buildAllNodesStageMap(allNodeDetailList, percentList, flowEntity, flowExtParams.getUrgencyType());

                Map<Long, Long> componentIdToConfigId = null;
                if (isContainConfigDeploy) {
                    Preconditions.checkState(!CollectionUtils.isEmpty(confInfoList), "ext params config list is empty");
                    componentIdToConfigId = confInfoList.stream().collect(
                            Collectors.toMap(ComponentConfInfo::getComponentId, ComponentConfInfo::getConfigId));
                }

                for (ComponentNodeDetail nodeDetail : allNodeDetailList) {
                    String hostname = nodeDetail.getHostName();
                    long componentId = nodeDetail.getComponentId();
                    hostNameToComponentId.put(hostname, componentId);
                    if (isContainConfigDeploy) {
                        final Long configId = componentIdToConfigId.get(componentId);
                        Preconditions.checkState(NumberUtils.isPositiveLong(configId), "not find config, componentId is: " + componentId);
                        hostNameToConfigId.put(hostname, configId);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("un-support scopeType:" + scopeType);
        }

        log.info("stageWithNodes is {}", JSONUtil.toJsonStr(stageWithNodes));
        if (isContainConfigDeploy) {
            log.info("hostNameToConfigId is {}", JSONUtil.toJsonStr(hostNameToConfigId));
        }
        if (isContainPackageDeploy) {
            log.info("deploy package id is {}", flowEntity.getPackageId());
        }

        final PipelineParameter pipelineParameter = new PipelineParameter();
        pipelineParameter.setFlowEntity(flowEntity);
        final List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(pipelineParameter);

        // 是否为多阶段类型发布
        boolean isStagedDeploy = stageWithNodes.size() > 1;
        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        Integer flowParallelism = flowEntity.getParallelism();
        final Long flowId = flowEntity.getId();

        int batchId = 0;
        int curs;
        int stageIndex = 0;
        for (Map.Entry<String, Set<String>> entry : stageWithNodes.entrySet()) {
            // String stage = entry.getKey();
            stageIndex ++;
            String stage = String.valueOf(stageIndex);
            // insert stage start logical node
            if (isStagedDeploy) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(stage);
                jobEntity.setFlowId(flowId);
                jobEntity.setNodeName("第" + stage + "阶段开始");
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                jobEntity.setBatchId(++batchId);
                jobEntity.setNodeType(NodeType.STAGE_START_NODE);
                executionNodeList.add(jobEntity);
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
                executionNodeList.add(jobEntity);
                if (++curs >= flowParallelism) {
                    curs = 0;
                    batchId++;
                }
            }

            // insert stage end logical node
            if (isStagedDeploy) {
                // batchId = executionNodeList.get(executionNodeList.size() - 1).getBatchId() + 1;
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage(stage);
                jobEntity.setFlowId(flowId);
                jobEntity.setNodeName("第" + stage + "阶段结束");
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                if (curs == 0) {
                    jobEntity.setBatchId(batchId);
                } else {
                    jobEntity.setBatchId(++batchId);
                }
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                jobEntity.setNodeType(NodeType.STAGE_END_NODE);
                executionNodeList.add(jobEntity);
            }
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("{}: save flow {} execution node list success.", getName(), flowId);

        for (ExecutionNodeEntity nodeEntity : executionNodeList) {
            final NNProxyDeployNodePreFillExtParams nodeProps = new NNProxyDeployNodePreFillExtParams();
            final String hostname = nodeEntity.getNodeName();
            // 过滤掉逻辑节点
            if (!nodeEntity.getNodeType().isNormalExecNode()) {
                continue;
            }
            Preconditions.checkState(hostNameToComponentId.containsKey(hostname), "hostname is not find: " + hostname);

            final Long nodeId = nodeEntity.getId();
            Preconditions.checkState(NumberUtils.isPositiveLong(nodeId), "node id not exists: " + JSONUtil.toJsonStr(nodeEntity));
            nodeProps.setNodeId(nodeId);
            if (isContainPackageDeploy) {
                nodeProps.setContainPackage(true);
                nodeProps.setPackageId(flowEntity.getPackageId());
            }

            if (isContainConfigDeploy) {
                nodeProps.setContainConfig(true);
                nodeProps.setConfigId(hostNameToConfigId.get(hostname));
            }
            nodeProps.setComponentId(hostNameToComponentId.get(hostname));
            nodePropsService.saveNodeProp(nodeId, nodeProps);
        }
        log.info("{}: save flow {} execution node list props success.", getName(), flowId);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("{}: save flow {} execution job event success.", getName(), flowId);
    }

    private Map<String, Set<String>> buildGrayStageMap(List<String> nodeList, List<Integer> percentList, FlowUrgencyType urgencyType) {
        switch (urgencyType) {
            case NORMAL:
                return StageSplitUtil.buildStageMap(nodeList, percentList);
            case EMERGENCY:
                Map<String, Set<String>> stageWithJobs = new LinkedHashMap<>();
                stageWithJobs.put(Constants.EMPTY_STRING, new LinkedHashSet<>(nodeList));
                return stageWithJobs;
            default:
                throw new IllegalArgumentException("unknown of urgencyType: " + urgencyType);
        }
    }

    private Map<String, Set<String>> buildAllNodesStageMap(List<ComponentNodeDetail> allNodeDetailList,
                                                           List<Integer> percentList,
                                                           ExecutionFlowEntity flowEntity,
                                                           FlowUrgencyType urgencyType) {
        final Long clusterId = flowEntity.getClusterId();
        Map<String, Set<String>> stageWithJobs = new LinkedHashMap<>();

        if (urgencyType == FlowUrgencyType.EMERGENCY) {
            stageWithJobs.put(Constants.EMPTY_STRING, allNodeDetailList.stream()
                    .map(ComponentNodeDetail::getHostName)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
            return stageWithJobs;
        }

        Map<Long, List<String>> componentIdToNodeList = new HashMap<>();
        allNodeDetailList.stream().forEach(node -> {
            String hostName = node.getHostName();
            long componentId = node.getComponentId();
            componentIdToNodeList.computeIfAbsent(componentId, key -> new ArrayList<>()).add(hostName);
        });

        List<MetadataComponentData> componentDataList = bmrMetadataService.queryComponentListByClusterId(clusterId);
        Preconditions.checkState(!CollectionUtils.isEmpty(componentDataList), "componentDataList is empty, cluster id is: " + clusterId);
        Map<NNProxyPriority, List<Long>> priorityWithComponentIdList = new TreeMap<>();
        componentDataList.stream().forEach(componentData -> {
            int priority = componentData.getPriority();
            long componentId = componentData.getId();
            NNProxyPriority.checkPriority(priority);
            priorityWithComponentIdList.computeIfAbsent(NNProxyPriority.getByValue(priority), k -> new ArrayList<>()).add(componentId);
        });
        final int size = priorityWithComponentIdList.size();

        Map<String, Set<String>> stageWithNodeList = new LinkedHashMap<>();

        Function func1 = stage -> Integer.parseInt(stage.toString());
        Function func2 = stage -> Integer.parseInt(stage.toString()) + 1;
        Function func3 = stage -> Integer.parseInt(stage.toString()) + 3;

        Map<Integer, Function<String, Integer>> funcMap = new HashMap<>();
        // 存在两个优先等级
        if (size == 2) {
            funcMap.put(1, func1);
            funcMap.put(2, func3);
        } else {
            funcMap.put(1, func1);
            funcMap.put(2, func2);
            funcMap.put(3, func3);
        }

        final AtomicInteger index = new AtomicInteger(0);
        for (Map.Entry<NNProxyPriority, List<Long>> entry : priorityWithComponentIdList.entrySet()) {
            index.incrementAndGet();
            List<Long> componentIdList = entry.getValue();
            if (CollectionUtils.isEmpty(componentIdList)) {
                continue;
            }
            for (Long componentId : componentIdList) {
                List<String> nodeList = componentIdToNodeList.get(componentId);
                if (CollectionUtils.isEmpty(nodeList)) {
                    continue;
                }
                final Map<String, Set<String>> stageMap = StageSplitUtil.buildStageMap(nodeList, percentList);
                stageMap.entrySet().stream().forEach(
                        e -> {
                            final String stageValue = e.getKey();
                            final Function<String, Integer> function = funcMap.get(index.get());
                            Preconditions.checkNotNull(function, "priority mapping func not support: " + entry.getKey());
                            final String stage = function.apply(stageValue).toString();
                            stageWithNodeList.computeIfAbsent(stage, k -> new LinkedHashSet<>()).addAll(e.getValue());
                        }
                );
            }
        }
        return stageWithNodeList;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.NNPROXY_DEPLOY);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.NN_PROXY_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
