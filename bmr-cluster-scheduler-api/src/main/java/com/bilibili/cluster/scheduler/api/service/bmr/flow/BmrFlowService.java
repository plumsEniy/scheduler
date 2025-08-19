package com.bilibili.cluster.scheduler.api.service.bmr.flow;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.dto.flow.UpdateExecutionFlowDTO;
import com.bilibili.cluster.scheduler.common.entity.BmrFlowEntity;
import com.bilibili.cluster.scheduler.common.entity.ExecutionFlowPropsEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;

/**
 * @description: bmr工作流的服务
 * @Date: 2024/5/23 10:37
 * @Author: nizhiqiang
 */
public interface BmrFlowService extends IService<BmrFlowEntity> {

    void alterFlowStatus(UpdateBmrFlowDto updateBmrFlowDto);

    /**
     * 变更工作流状态
     *
     * @param flowId
     * @param operate
     */
    void alterFlowStatus(Long flowId, FlowOperateButtonEnum operate);
}
