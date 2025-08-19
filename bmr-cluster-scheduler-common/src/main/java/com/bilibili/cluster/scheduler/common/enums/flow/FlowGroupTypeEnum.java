package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.Getter;

/**
 * @description: 分组方式
 * @Date: 2024/5/9 17:17
 * @Author: nizhiqiang
 */
public enum FlowGroupTypeEnum {

    RACK_GROUP("按机架分组"),
    RANDOM_GROUP("随机分组");

    @Getter
    String desc;

    FlowGroupTypeEnum(String desc) {
        this.desc = desc;
    }
}
