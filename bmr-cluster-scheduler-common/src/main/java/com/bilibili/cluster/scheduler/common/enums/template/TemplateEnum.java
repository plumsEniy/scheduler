package com.bilibili.cluster.scheduler.common.enums.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 模版类型
 * @Date: 2024/6/7 11:01
 * @Author: nizhiqiang
 */
@Getter
@AllArgsConstructor
public enum TemplateEnum {
    PRESTO("PrestoTemplate.ftl"),
    CLICKHOUSE("ClickhouseTemplate.ftl"),
    ;

    String path;
}
