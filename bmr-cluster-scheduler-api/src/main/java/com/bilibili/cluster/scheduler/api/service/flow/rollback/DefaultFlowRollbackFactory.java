package com.bilibili.cluster.scheduler.api.service.flow.rollback;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class DefaultFlowRollbackFactory implements FlowRollbackFactory {

    @Override
    public boolean supportRollback(ExecutionFlowEntity flowEntity) {
        return false;
    }

    @Override
    public List<FlowOperateButtonEnum> getRollbackButton(ExecutionFlowEntity flowEntity) {
        return Collections.emptyList();
    }

    @Override
    public boolean doRollback(FlowOperateButtonEnum buttonType, ExecutionFlowEntity flowEntity) {
        return false;
    }

    @Override
    public List<FlowDeployType> fitDeployType() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public void handlerUpdateFlowStatus(ExecutionFlowEntity flowEntity) {
        throw new RuntimeException("Un-support");
    }
}
