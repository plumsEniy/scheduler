package com.bilibili.cluster.scheduler.api.service.flow.rollback.bus;

import com.bilibili.cluster.scheduler.api.service.flow.rollback.FlowRollbackFactory;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.api.service.flow.rollback.DefaultFlowRollbackFactory;

public interface FlowRollbackBusFactoryService {

    /**
     * 根据工作流类型获取指定的回滚处理工厂类，
     * 默认实现 {@link DefaultFlowRollbackFactory}, 不支持回滚能力
     * @param flowEntity
     * @return
     */
    FlowRollbackFactory getRollbackFactory(ExecutionFlowEntity flowEntity);

}
