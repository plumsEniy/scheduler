package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SubDeployType {

    CAPACITY_EXPANSION("扩容", FlowDeployType.CAPACITY_EXPANSION),

    ITERATION_RELEASE("迭代", FlowDeployType.ITERATION_RELEASE),
    ;

    @Getter
    String desc;

    @Getter
    FlowDeployType flowDeployType;

}
