package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowAopEventEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionNodeEntity;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;

/**
 * <p>
 * 工作流切片事件 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2025-05-12
 */
public interface ExecutionFlowAopEventService extends IService<ExecutionFlowAopEventEntity> {

    /**
     * 初始化工作流切片事件
     *
     * @param flowEntity
     */
    void initFlowAopEvent(ExecutionFlowEntity flowEntity);

    /**
     * 创建工作流切片事件
     *
     * @param flowEntity
     */
    void createFlowAop(ExecutionFlowEntity flowEntity);

    /**
     * 开始工作流
     *
     * @param flowEntity
     */
    void startFlowAop(ExecutionFlowEntity flowEntity);

    /**
     * 放弃工作流
     *
     * @param flowEntity
     */
    void giveUpFlowAop(ExecutionFlowEntity flowEntity);

    /**
     * 任务失败
     *
     * @param flowEntity
     * @param executionNodeEntityList
     */
    void jobFailAop(ExecutionFlowEntity flowEntity, ExecutionNodeEntity executionNode, String errorMsg);

    /**
     * 任务完成
     *
     * @param flowEntity
     * @param executionNodeEntityList
     */
    void jobFinishAop(ExecutionFlowEntity flowEntity, ExecutionNodeEntity executionNode);

    /**
     * 结束工作流
     *
     * @param flowEntity
     */
    void finishFlowAop(ExecutionFlowEntity flowEntity, FlowStatusEnum beforeStatus);
}
