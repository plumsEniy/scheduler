package com.bilibili.cluster.scheduler.common.enums.flowLog;

public enum LogTypeEnum {

    // 集成任务链路内部使用到的kafka topic 相关操作
    FLOW("FLOW级别日志"),

    EVENT("事件级别日志");


    private String desc;

    LogTypeEnum(String desc) {
        this.desc = desc;
    }

}
