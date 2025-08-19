package com.bilibili.cluster.scheduler.api.service.tide;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.TideNodeDetail;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TideDynamicNodeGenerateServiceImpl implements TideDynamicNodeGenerateService {

    @Resource
    ExecutionFlowService flowService;

    @Resource
    ExecutionNodeService executionNodeService;

    @Resource
    ExecutionNodeEventService executionNodeEventService;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean generateTideStage2NodeAndEvents(List<TideNodeDetail> tideNodeList, long flowId, FlowPrepareGenerateFactory flowPrepareGenerateFactory) throws Exception {
        if (CollectionUtils.isEmpty(tideNodeList)) {
            return false;
        }
        final ExecutionFlowEntity flowEntity = flowService.getById(flowId);
        List<ExecutionNodeEntity> executionNodeList = new ArrayList<>();
        for (TideNodeDetail nodeDetail : tideNodeList) {
            final ExecutionNodeEntity nodeEntity = new ExecutionNodeEntity();
            String hostname = nodeDetail.getHostName();
            nodeEntity.setNodeName(hostname);
            nodeEntity.setBatchId(2);
            nodeEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            nodeEntity.setFlowId(flowId);
            nodeEntity.setExecStage("2");
            nodeEntity.setOperator(flowEntity.getOperator());
            nodeEntity.setOperationResult(NodeOperationResult.NORMAL);
            nodeEntity.setIp(nodeDetail.getIp());
            executionNodeList.add(nodeEntity);
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionNodeList, 100);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(executionNodeService.batchInsert(split), "批量插入execution node失败");
        }

        final List<ResolvedEvent> resolvedEventList = flowPrepareGenerateFactory.resolvePipelineEventList(null, flowEntity, null, null);
        log.info("{}#resolvePipelineEventList is {}", flowPrepareGenerateFactory.getName(), resolvedEventList);

        executionNodeEventService.batchSaveExecutionNodeEvent(flowId, executionNodeList, resolvedEventList);
        log.info("save flow {} stage2 nodes and events success.", flowId);

        flowService.updateMaxBatchId(flowId, 2);
        // update flow tolerance to 2 nodes
        if (tideNodeList.size() > 16) {
            flowService.updateFlowTolerance(flowId, 2);
        }
        return true;
    }
}
