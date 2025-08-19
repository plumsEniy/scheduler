package com.bilibili.cluster.scheduler.common.enums.bmr.flow;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 工作流操作状态
 * @Date: 2024/5/23 16:41
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum BmrFlowOpStrategy {
    PROCEED_ALL("继续全部"),
    TERMINATED("终止"),
    PAUSE("暂停"),
    FAILED_PAUSE("出错并暂停"),
    DONE_BUT_NOT_FINISH("完成但未接单"),

    PROCEED_ROLLBACK("回滚中"),
    ;
    @Getter
    String desc;

}
