package com.bilibili.cluster.scheduler.common.enums.flow;

import lombok.Getter;

/**
 * @description: 包类型
 * @Date: 2024/5/9 17:07
 * @Author: nizhiqiang
 */
public enum FlowDeployPackageType {
    SERVICE_PACKAGE("安装包发布"),
    CONFIG_PACKAGE("配置包发布"),
    ALL_PACKAGE("混合包发布"),
    NONE_PACKAGE("非包变更发布"),

    ITERATION_RELEASE_LABEL("超配比发布");

    @Getter
    String desc;

    FlowDeployPackageType(String desc) {
        this.desc = desc;
    }
}
