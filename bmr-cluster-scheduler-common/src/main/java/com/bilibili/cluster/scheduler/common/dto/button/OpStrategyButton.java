package com.bilibili.cluster.scheduler.common.dto.button;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowOperateButtonEnum;
import lombok.Data;

/**
 * @description: 操作按钮
 * @Date: 2024/2/1 15:54
 * @Author: nizhiqiang
 */
@Data
public class OpStrategyButton {
    private FlowOperateButtonEnum button;
    private boolean state;
}
