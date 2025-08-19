package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.Getter;

/**
 * @description: 生效模式
 * @Date: 2024/5/9 17:11
 * @Author: nizhiqiang
 */
public enum FlowEffectiveModeEnum {


    RESTART_EFFECTIVE("重启生效"),
    IMMEDIATE_EFFECTIVE("即刻生效"),
    ;

    @Getter
    private String desc;

    FlowEffectiveModeEnum(String desc) {
        this.desc = desc;
    }
}
