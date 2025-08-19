package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory;

import cn.hutool.core.collection.ListUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @description:
 * @Date: 2024/12/30 15:46
 * @Author: nizhiqiang
 */

@Slf4j
@Component
public class HboJobParamsUpdateFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

    @Resource
    ExecutionFlowPropsService executionFlowPropsService;

    @Resource
    ExecutionNodeService nodeService;

    @Resource
    ExecutionNodeEventService nodeEventService;

    @Override
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        Long flowId = flowEntity.getId();
        List<ExecutionNodeEntity> executionJobList = new ArrayList<>();
        BaseFlowExtPropDTO flowProps = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        List<String> jobIdList = flowProps.getNodeList();

        Integer flowParallelism = flowEntity.getParallelism();
        int batchId = 1;
        int curs = 0;

        for (String jobId : jobIdList) {
            ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
            jobEntity.setNodeName(jobId);
            jobEntity.setFlowId(flowId);
            jobEntity.setOperator(flowEntity.getOperator());
            jobEntity.setNodeStatus(NodeExecuteStatusEnum.UN_NODE_EXECUTE);
            jobEntity.setBatchId(batchId);
            jobEntity.setRack(Constants.EMPTY_STRING);
            jobEntity.setOperationResult(NodeOperationResult.NORMAL);
            if (++curs >= flowParallelism) {
                curs = 0;
                batchId++;
            }
            executionJobList.add(jobEntity);
        }

        List<List<ExecutionNodeEntity>> splitList = ListUtil.split(executionJobList, 500);
        for (List<ExecutionNodeEntity> split : splitList) {
            Assert.isTrue(nodeService.batchInsert(split), "批量插入execution node失败");
        }
        log.info("save flow {} execution node list success.", flowId);

        List<ResolvedEvent> resolvedEventList = resolvePipelineEventList(null, flowEntity, null, null);
        log.info("hbo job params event list is {}", resolvedEventList);

        nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
        log.info("save flow {} execution job event success.", flowId);
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.HBO_JOB_PARAM_RULE_UPDATE);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.HBO_JOB_PARAMS_UPDATE_DEPLOY_FACTORY_IDENEITFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
