package com.bilibili.cluster.scheduler.common.enums.props;

/**
 * @description: 属性枚举
 * @Date: 2024/5/10 16:24
 * @Author: nizhiqiang
 */
public enum PropsTypeEnum {

    // 集成任务链路内部使用到的kafka topic 相关操作
    FLOW("FLOW级别参数"),

    NODE("节点级别参数"),
    EVENT("事件级别参数"),
    ;

    private String desc;

    PropsTypeEnum(String desc) {
        this.desc = desc;
    }
}
