package com.bilibili.cluster.scheduler.api.service.flow.rollback.bus;

import com.bilibili.cluster.scheduler.api.service.flow.rollback.FlowRollbackFactory;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流回滚总线，获取不同工作流的回滚工厂类
 */
@Slf4j
@Component
public class FlowRollbackBusFactoryServiceImpl implements FlowRollbackBusFactoryService, InitializingBean {

    @Resource
    List<FlowRollbackFactory> rollbackFactoryList;

    private Map<FlowDeployType, FlowRollbackFactory> rollbackFactoryHolder = new HashMap<>();
    private Map<String, FlowRollbackFactory> namedFactoryHolder = new HashMap<>();

    @Override
    public FlowRollbackFactory getRollbackFactory(ExecutionFlowEntity flowEntity) {
        final FlowDeployType deployType = flowEntity.getDeployType();
        final FlowRollbackFactory rollbackFactory = rollbackFactoryHolder.get(deployType);
        if (rollbackFactory.isDefault()) {
            return reChoiceByExtFlowInfo(rollbackFactory, flowEntity);
        }
        return rollbackFactory;
    }

    private FlowRollbackFactory reChoiceByExtFlowInfo(FlowRollbackFactory defaultRollbackFactory, ExecutionFlowEntity flowEntity) {
        final String componentName = flowEntity.getComponentName();
        final FlowDeployType deployType = flowEntity.getDeployType();
        // NN-Proxy的迭代发布支持回滚
//        if (componentName.equals("NNProxy") && deployType.equals(FlowDeployType.ITERATION_RELEASE)) {
//            return namedFactoryHolder.get("");
//        }
        // 按需获取定制化的回滚工厂类

        // 默认不支持回滚
        return defaultRollbackFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        FlowRollbackFactory defaultFactory = null;
        for (FlowRollbackFactory rollbackFactory : rollbackFactoryList) {
            List<FlowDeployType> flowDeployTypes = rollbackFactory.fitDeployType();
            if (!CollectionUtils.isEmpty(flowDeployTypes)) {
                flowDeployTypes.forEach(type -> rollbackFactoryHolder.put(type, rollbackFactory));
            }
            namedFactoryHolder.put(rollbackFactory.getName(), rollbackFactory);
            if (rollbackFactory.isDefault()) {
                defaultFactory = rollbackFactory;
            }
        }

        Preconditions.checkNotNull(defaultFactory, "default rollback factory is null, please check");
        for (FlowDeployType deployType : FlowDeployType.values()) {
            if (!rollbackFactoryHolder.containsKey(deployType)) {
                rollbackFactoryHolder.put(deployType, defaultFactory);
            }
        }

    }
}
