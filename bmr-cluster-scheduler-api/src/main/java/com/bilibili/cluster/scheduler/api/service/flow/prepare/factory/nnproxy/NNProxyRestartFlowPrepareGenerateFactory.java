package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.bmr.resource.BmrResourceService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.ComponentNodeDetail;
import com.bilibili.cluster.scheduler.common.dto.bmr.resource.req.QueryComponentNodeListReq;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NNProxyRestartFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowPropsService flowPropsService;

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodePropsService nodePropsService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Resource
    BmrResourceService bmrResourceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        BaseFlowExtPropDTO flowProps = flowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        final List<String> nodeList = flowProps.getNodeList();

        final String releaseScopeType = flowEntity.getReleaseScopeType();
        FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);

        final PipelineParameter pipelineParameter = new PipelineParameter();
        pipelineParameter.setFlowEntity(flowEntity);
        final List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(pipelineParameter);

        final QueryComponentNodeListReq req = new QueryComponentNodeListReq(flowEntity.getClusterId(), null, null);
        req.setNeedDns(true);
        if (!scopeType.equals(FlowReleaseScopeType.FULL_RELEASE)) {
            req.setHostNameList(nodeList);
        }
        final List<ComponentNodeDetail> restartNodeDetailList = bmrResourceService.queryNodeList(req);
        Preconditions.checkState(!CollectionUtils.isEmpty(restartNodeDetailList),
                "restartNodeDetailList is empty, cluster id is: " + flowEntity.getClusterId());
        final Map<String, Map<String, List<String>>> dnsMap = new TreeMap<>();
        for (ComponentNodeDetail componentNodeDetail : restartNodeDetailList) {
            final String componentName = componentNodeDetail.getComponentName();
            final String dns = componentNodeDetail.getDns();
            final String hostName = componentNodeDetail.getHostName();
            Preconditions.checkState(!StringUtils.isBlank(dns),
                    hostName + " dns is blank, please check");
            dnsMap.computeIfAbsent(componentName, k -> new TreeMap<>())
                    .computeIfAbsent(dns, v -> new ArrayList<>())
                    .add(hostName);
        }
        Map<Integer, List<String>> batchToHostList = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> componentEntry : dnsMap.entrySet()) {
            Map<String, List<String>> dnsToHostList = componentEntry.getValue();
            int index = 0;
            for (Map.Entry<String, List<String>> dnsEntry : dnsToHostList.entrySet()) {
                index++;
                batchToHostList.computeIfAbsent(index, i -> new ArrayList<>()).addAll(dnsEntry.getValue());
            }
        }

        int nextBatchId = 0;
        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();

        for (Map.Entry<Integer, List<String>> entry : batchToHostList.entrySet()) {
            nextBatchId++;
            final List<String> curBatchNodeList = entry.getValue();
            for (String nodeName : curBatchNodeList) {
                final ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setExecStage("1");
                jobEntity.setFlowId(flowId);
                jobEntity.setNodeName(nodeName);
                jobEntity.setOperator(flowEntity.getOperator());
                jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
                jobEntity.setBatchId(nextBatchId);
                jobEntity.setOperationResult(NodeOperationResult.NORMAL);
                executionNodeList.add(jobEntity);
            }
        }
        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("{}: save flow {} execution node list success.", getName(), flowId);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("{}: save flow {} execution job event success.", getName(), flowId);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.NNPROXY_RESTART);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(
                Constants.NN_PROXY_RESTART_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
