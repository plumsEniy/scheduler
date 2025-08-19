package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @description: 工作流操作策略
 * @Date: 2024/2/1 17:33
 * @Author: nizhiqiang
 */
@AllArgsConstructor
public enum FlowOperateButtonEnum {
    PROCEED("继续"),
    PAUSE("暂停"),
    SKIP_FAILED_AND_PROCESS("跳过错误并继续"),
    TERMINATE("结单"),

    FULL_ROLLBACK("全量回滚"),
    STAGED_ROLLBACK("阶段回滚"),
    ;

    @Getter
    String desc;

}
