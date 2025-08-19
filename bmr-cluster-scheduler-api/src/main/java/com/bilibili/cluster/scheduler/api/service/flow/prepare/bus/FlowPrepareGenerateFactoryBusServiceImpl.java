package com.bilibili.cluster.scheduler.api.service.flow.prepare.bus;

import com.bilibili.cluster.scheduler.api.event.analyzer.PipelineParameter;
import com.bilibili.cluster.scheduler.api.service.bmr.metadata.BmrMetadataService;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.CommonDeployFlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.FlowPrepareGenerateFactory;
import com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.zookeeper.ZkDeployFlowPrepareGeneratorFactory;
import com.bilibili.cluster.scheduler.common.Constants;
import com.bilibili.cluster.scheduler.common.dto.flow.req.DeployOneFlowReq;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class FlowPrepareGenerateFactoryBusServiceImpl implements FlowPrepareGenerateFactoryBusService, InitializingBean {

    @Resource
    List<FlowPrepareGenerateFactory> prepareGenerateFactories;

    private Map<FlowDeployType, FlowPrepareGenerateFactory> deployFactoryHolder = new HashMap<>();
    private Map<String, FlowPrepareGenerateFactory> namedFactoryHolder = new HashMap<>();

    @Resource
    private BmrMetadataService bmrMetadataService;


    @Override
    public FlowPrepareGenerateFactory forwardFlowPrepareGenerateFactory(PipelineParameter pipelineParameter) {
        final ExecutionFlowEntity flowEntity = pipelineParameter.getFlowEntity();
        if (!Objects.isNull(flowEntity)) {
            return forwardFlowPrepareGenerateFactory(flowEntity);
        }

        final DeployOneFlowReq req = pipelineParameter.getReq();
        final FlowDeployType deployType = req.getDeployType();

        final FlowPrepareGenerateFactory flowPrepareGenerateFactory = deployFactoryHolder.get(deployType);
        if (flowPrepareGenerateFactory instanceof CommonDeployFlowPrepareGenerateFactory) {
            return reForwardByHitCommonFactory(flowEntity, (CommonDeployFlowPrepareGenerateFactory) flowPrepareGenerateFactory);
        } else {
            return flowPrepareGenerateFactory;
        }
    }

    @Override
    public FlowPrepareGenerateFactory forwardFlowPrepareGenerateFactory(ExecutionFlowEntity flowEntity) {
        final FlowDeployType deployType = flowEntity.getDeployType();
        final FlowPrepareGenerateFactory flowPrepareGenerateFactory = deployFactoryHolder.get(deployType);
        if (flowPrepareGenerateFactory instanceof CommonDeployFlowPrepareGenerateFactory) {
            return reForwardByHitCommonFactory(flowEntity, (CommonDeployFlowPrepareGenerateFactory) flowPrepareGenerateFactory);
        } else {
            return flowPrepareGenerateFactory;
        }
    }

    // TODO: for custom FlowPrepareGenerateFactory
    private FlowPrepareGenerateFactory reForwardByHitCommonFactory(ExecutionFlowEntity executionFlow,
                                                                   CommonDeployFlowPrepareGenerateFactory commonDeployFlowPrepareGenerateFactory) {
        FlowDeployType deployType = executionFlow.getDeployType();
        String clusterName = StringUtils.isEmpty(executionFlow.getClusterName()) ? Constants.EMPTY_STRING : executionFlow.getClusterName();
        String componentName = StringUtils.isEmpty(executionFlow.getComponentName()) ? Constants.EMPTY_STRING : executionFlow.getComponentName();
        String upperService = StringUtils.isEmpty(executionFlow.getRoleName()) ? Constants.EMPTY_STRING : executionFlow.getRoleName();

        if (deployType == FlowDeployType.CAPACITY_EXPANSION && componentName.equalsIgnoreCase("NameNode")) {
            // return namedFactoryHolder.get("NameNodeExpansionFlowPrepareGenerateFactory");
        }

        if (componentName.equals(Constants.ZK_COMPONENT)) {
            return namedFactoryHolder.get(ZkDeployFlowPrepareGeneratorFactory.class.getSimpleName());
        }

        switch (deployType) {
            case K8S_CAPACITY_EXPANSION:
            case K8S_ITERATION_RELEASE:
                if (Constants.CLICK_HOUSE_COMPONENT.equals(componentName)) {
                    return namedFactoryHolder.get("ClickHouseContainerPrepareGenerateFactory");
                }
                break;
            default:
        }

        return commonDeployFlowPrepareGenerateFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        FlowPrepareGenerateFactory defaultFactory = null;
        for (FlowPrepareGenerateFactory factory : prepareGenerateFactories) {
            namedFactoryHolder.put(factory.getName(), factory);
            if (factory.isDefault()) {
                defaultFactory = factory;
            }
            List<FlowDeployType> deployTypeList = factory.fitDeployType();
            if (!CollectionUtils.isEmpty(deployTypeList)) {
                deployTypeList.forEach(
                        deployType -> deployFactoryHolder.put(deployType, factory)
                );
            }
        }

        Preconditions.checkNotNull(defaultFactory, "default of FlowPrepareGenerateFactory is null");
        for (FlowDeployType deployType : FlowDeployType.values()) {
            if (!deployFactoryHolder.containsKey(deployType)) {
                deployFactoryHolder.put(deployType, defaultFactory);
            }
        }
    }
}
