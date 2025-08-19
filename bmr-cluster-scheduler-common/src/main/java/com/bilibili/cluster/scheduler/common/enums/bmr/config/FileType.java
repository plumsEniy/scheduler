package com.bilibili.cluster.scheduler.common.enums.bmr.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 文件类型
 * @Date: 2024/4/24 16:51
 * @Author: nizhiqiang
 */
@AllArgsConstructor
@Getter
public enum FileType {
    LIST( "list 类型"),

    JSON("json 类型"),

    MAP("map 类型"),

    ;
    private String desc;
}
