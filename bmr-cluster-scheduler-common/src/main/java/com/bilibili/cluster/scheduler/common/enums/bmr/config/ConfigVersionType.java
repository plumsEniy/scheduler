package com.bilibili.cluster.scheduler.common.enums.bmr.config;

import lombok.AllArgsConstructor;

/**
 * @description: 配置版本的文件类型
 * @Date: 2024/6/7 15:00
 * @Author: nizhiqiang
 */
@AllArgsConstructor
public enum ConfigVersionType {
    NORMAL("普通"),
    SPECIAL("特殊类型");

    String desc;
}
