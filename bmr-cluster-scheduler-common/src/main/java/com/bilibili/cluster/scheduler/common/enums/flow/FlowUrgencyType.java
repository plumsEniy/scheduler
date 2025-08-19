package com.bilibili.cluster.scheduler.common.enums.flow;


import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public enum FlowUrgencyType {

    NORMAL("普通发布"),

    EMERGENCY("紧急发布"),

    ;
    @Getter
    String desc;


}
