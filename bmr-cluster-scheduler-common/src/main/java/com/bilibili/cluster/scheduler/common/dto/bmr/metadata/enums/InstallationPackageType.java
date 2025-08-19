package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums;

import lombok.AllArgsConstructor;

/**
 * @description: 安装包类型
 * @Date: 2025/4/28 16:46
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum InstallationPackageType {
    COMPONENT("组件类型"),
    PLUGIN("插件类型");


    private String description;
}
