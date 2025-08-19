package com.bilibili.cluster.scheduler.api.service.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.dto.button.StageStateEnum;
import com.bilibili.cluster.scheduler.common.dto.flow.SaberUpdateProp;
import com.bilibili.cluster.scheduler.common.dto.spark.params.SparkDeployFlowExtParams;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowPropsEntity;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 谢谢谅解
 * @since 2024-01-26
 */
public interface ExecutionFlowPropsService extends IService<ExecutionFlowPropsEntity> {

    /**
     * 根据工作流id查询变更参数
     *
     * @param flowId
     * @return
     */
    <T> T getFlowPropByFlowId(Long flowId, Class<T> clazz);

    <T> T getFlowPropByFlowIdWithCache(Long flowId, Class<T> clazz);

    void saveFlowProp(Long flowId, Object obj);

    <T> T getFlowExtParamsByCache(Long flowId, Class<T> tClass);

    boolean updateStageInfo(Long flowId, String execStage, StageStateEnum stageState,
                            LocalDateTime startTime, LocalDateTime endTime,
                            LocalDateTime allowedNextStageStartTime);
}
