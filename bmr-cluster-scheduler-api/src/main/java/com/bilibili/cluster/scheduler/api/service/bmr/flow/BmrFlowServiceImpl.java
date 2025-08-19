package com.bilibili.cluster.scheduler.api.service.bmr.flow;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bilibili.cluster.scheduler.common.dto.bmr.flow.UpdateBmrFlowDto;
import com.bilibili.cluster.scheduler.common.entity.BmrFlowEntity;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import com.bilibili.cluster.scheduler.dao.mapper.BmrFlowMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @description:
 * @Date: 2024/5/23 10:38
 * @Author: nizhiqiang
 */
@Service
@Slf4j
public class BmrFlowServiceImpl extends ServiceImpl<BmrFlowMapper, BmrFlowEntity> implements BmrFlowService {

    @Override
    public void alterFlowStatus(UpdateBmrFlowDto updateBmrFlowDto) {
        LambdaUpdateWrapper<BmrFlowEntity> updateWrapper = new UpdateWrapper<BmrFlowEntity>().lambda();
        updateWrapper.eq(BmrFlowEntity::getId, updateBmrFlowDto.getFlowId());
        log.info("update bmr flow dto is {}", JSONUtil.toJsonStr(updateBmrFlowDto));
        if (updateBmrFlowDto.getFlowStatus() != null) {
            updateWrapper.set(BmrFlowEntity::getFlowState, updateBmrFlowDto.getFlowStatus().toString());
        }

        if (updateBmrFlowDto.getOpStrategy() != null) {
            updateWrapper.set(BmrFlowEntity::getOpStrategy, updateBmrFlowDto.getOpStrategy().toString());
        }

        if (updateBmrFlowDto.getStartTime() != null) {
            updateWrapper.set(BmrFlowEntity::getStartTime, updateBmrFlowDto.getStartTime());
        }

        if (updateBmrFlowDto.getEndTime() != null) {
            updateWrapper.set(BmrFlowEntity::getEndTime, updateBmrFlowDto.getEndTime());
        }

        if (!StringUtils.isBlank(updateBmrFlowDto.getApplyState())) {
            updateWrapper.set(BmrFlowEntity::getApplyState, updateBmrFlowDto.getApplyState());
        }

        update(updateWrapper);
    }

    @Override
    public void alterFlowStatus(Long flowId, FlowOperateButtonEnum operate) {
        LambdaUpdateWrapper<BmrFlowEntity> updateWrapper = new UpdateWrapper<BmrFlowEntity>().lambda();
        updateWrapper.eq(BmrFlowEntity::getId, flowId);
        switch (operate) {
            case PAUSE:
                updateWrapper.set(BmrFlowEntity::getOpStrategy, BmrFlowOpStrategy.PAUSE.toString());
                break;
            case PROCEED:
            case SKIP_FAILED_AND_PROCESS:
                updateWrapper.set(BmrFlowEntity::getOpStrategy, BmrFlowOpStrategy.PROCEED_ALL.toString())
                        .set(BmrFlowEntity::getFlowState, BmrFlowStatus.RUNNING.toString());
                break;
            case TERMINATE:
                updateWrapper.set(BmrFlowEntity::getOpStrategy, BmrFlowOpStrategy.TERMINATED.toString())
                        .set(BmrFlowEntity::getFlowState, BmrFlowStatus.SUCCESS.toString())
                        .set(BmrFlowEntity::getEndTime, LocalDateTime.now());
                break;
            case STAGED_ROLLBACK:
            case FULL_ROLLBACK:
                updateWrapper.set(BmrFlowEntity::getOpStrategy, BmrFlowOpStrategy.PROCEED_ROLLBACK.toString())
                        .set(BmrFlowEntity::getFlowState, BmrFlowStatus.RUNNING.toString());
                break;
        }
        update(updateWrapper);

    }
}
