package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:
 * @Date: 2024/5/20 14:54
 * @Author: nizhiqiang
 */
@AllArgsConstructor
@Getter
public enum HostType {

    VIRTUAL("虚拟机"),
    PHYSICAL("物理机"),

    @JsonEnumDefaultValue
    UNKNOWN("未知");

    private String desc;

}
