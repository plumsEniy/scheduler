package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.spark;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.event.analyzer.ResolvedEvent;
import com.bilibili.cluster.scheduler.api.event.factory.FactoryDiscoveryUtils;
import com.bilibili.cluster.scheduler.api.event.factory.PipelineFactory;
import com.bilibili.cluster.scheduler.api.service.GlobalService;
import com.bilibili.cluster.scheduler.api.service.bmr.spark.SparkManagerService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionFlowPropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeEventService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodePropsService;
import com.bilibili.cluster.scheduler.api.service.flow.ExecutionNodeService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataClusterData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataComponentData;
import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.MetadataPackageData;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployExtParams;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientDeployType;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientPackInfo;
import com.bilibili.cluster.scheduler.common.dto.spark.client.SparkClientType;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import com.bilibili.cluster.scheduler.common.enums.node.NodeOperationResult;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SparkClientDeployFlowPrepareGenerateFactory implements FlowPrepareGenerateFactory {

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
    GlobalService globalService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateNodeAndEvents(ExecutionFlowEntity flowEntity) throws Exception {
        final Long flowId = flowEntity.getId();
        final BaseFlowExtPropDTO flowProps = executionFlowPropsService.getFlowPropByFlowId(flowId, BaseFlowExtPropDTO.class);
        String flowExtParams = flowProps.getFlowExtParams();
        try {
            SparkClientDeployExtParams clientDeployExtParams = JSONUtil.toBean(flowExtParams, SparkClientDeployExtParams.class);
            final String releaseScopeType = flowEntity.getReleaseScopeType();
            final FlowReleaseScopeType scopeType = FlowReleaseScopeType.valueOf(releaseScopeType);
            final FlowDeployType deployType = flowEntity.getDeployType();
            final SparkClientDeployType packDeployType = clientDeployExtParams.getPackDeployType();
            // check client package Id is useful
            List<String> nodeList = flowProps.getNodeList();
            switch (packDeployType) {
                case ADD_NEWLY_VERSION:
                case REMOVE_USELESS_VERSION:
                    if (scopeType == FlowReleaseScopeType.FULL_RELEASE) {
                        nodeList = sparkManagerService.querySparkClientAllNodes();
                        if (CollectionUtils.isEmpty(nodeList)) {
                            return;
                        }
                    }
                    break;
            }
            Preconditions.checkState(!CollectionUtils.isEmpty(nodeList), "spark client node list is empty.");

            final List<Long> packIdList = clientDeployExtParams.getPackIdList();
            if (!CollectionUtils.isEmpty(packIdList)) {
                List<SparkClientPackInfo> packInfoList = new ArrayList<>();
                for (Long packId : packIdList) {
                    final MetadataPackageData packageData = globalService.getBmrMetadataService().queryPackageDetailById(packId);
                    final String upperService = packageData.getUpperService();
                    final SparkClientType clientType = SparkClientType.getByComponentName(upperService);

                    final SparkClientPackInfo clientPackInfo = new SparkClientPackInfo();
                    clientPackInfo.setPackName(packageData.getTagName());
                    clientPackInfo.setPackMd5(packageData.getProductBagMd5());
                    clientPackInfo.setDownloadUrl(packageData.getStoragePath());
                    clientPackInfo.setClientType(clientType.getDesc());

                    packInfoList.add(clientPackInfo);
                }
                Collections.sort(packInfoList);
                clientDeployExtParams.setPackInfoList(packInfoList);
                flowProps.setFlowExtParams(JSONUtil.toJsonStr(clientDeployExtParams));

                executionFlowPropsService.saveFlowProp(flowId, flowProps);
            }

            List<ExecutionNodeEntity> executionJobList = new ArrayList<>();
            Integer flowParallelism = flowEntity.getParallelism();
            int batchId = 1;
            int curs = 0;

            for (String nodeName : nodeList) {
                ExecutionNodeEntity jobEntity = new ExecutionNodeEntity();
                jobEntity.setNodeName(nodeName);
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
            log.info("spark client package deploy event list is {}", resolvedEventList);

            nodeEventService.batchSaveExecutionNodeEvent(flowId, executionJobList, resolvedEventList);
            log.info("save flow {} execution node event success.", flowId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Arrays.asList(FlowDeployType.SPARK_CLIENT_PACKAGE_DEPLOY);
    }

    @Override
    public List<ResolvedEvent> resolvePipelineEventList(PipelineParameter pipelineParameter) throws Exception {
        PipelineFactory pipelineFactory = FactoryDiscoveryUtils.getFactoryByIdentifier(Constants.SPARK_CLIENT_PACKAGE_DEPLOY_FACTORY_IDENTIFY, PipelineFactory.class);
        List<ResolvedEvent> resolvedEventList = pipelineFactory.analyzerAndResolveEvents(pipelineParameter);
        return resolvedEventList;
    }

}
