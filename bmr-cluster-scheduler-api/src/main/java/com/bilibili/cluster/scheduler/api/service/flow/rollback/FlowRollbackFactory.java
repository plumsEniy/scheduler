package com.bilibili.cluster.scheduler.api.service.flow.rollback;

import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;

import java.util.List;

public interface FlowRollbackFactory {

    /**
     * 是否支持回滚
     * @param flowEntity
     * @return
     */
    boolean supportRollback(ExecutionFlowEntity flowEntity);

    /**
     * 支持的回滚类别，当前仅支持:
     *     FULL_ROLLBACK("全量回滚"),
     *     STAGED_ROLLBACK("阶段回滚"),
     * @param flowEntity
     * @return
     */
    List<FlowOperateButtonEnum> getRollbackButton(ExecutionFlowEntity flowEntity);

    /**
     * 执行回滚类别
     * @param buttonType
     * @return
     */
    boolean doRollback(FlowOperateButtonEnum buttonType, ExecutionFlowEntity flowEntity) throws Exception;

    /**
     * 适用的发布类型列表
     * @return
     */
    List<FlowDeployType> fitDeployType();

    default String getName() {
        return getClass().getSimpleName();
    }

    default boolean isDefault() {
        return false;
    }

    void handlerUpdateFlowStatus(ExecutionFlowEntity flowEntity);

}
