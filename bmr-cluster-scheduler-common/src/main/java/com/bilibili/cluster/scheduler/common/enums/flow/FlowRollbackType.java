package com.bilibili.cluster.scheduler.common.enums.flow;

public enum FlowRollbackType {

    PREPARE_GLOBAL, // 等待回滚中

    GLOBAL, // 全局回滚

    STAGE,  // 阶段回滚

    NONE,   // 无

    ;


}
