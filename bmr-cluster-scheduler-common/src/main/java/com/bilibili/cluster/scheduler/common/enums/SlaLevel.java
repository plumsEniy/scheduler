package com.bilibili.cluster.scheduler.common.enums;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @description: 任务level
 * @Date: 2023/7/25 20:03
 * @Author: xiexieliangjie
 */
@Slf4j
@ToString
public enum SlaLevel {

    S(4, "S"),
    A(3, "A"),
    B(2, "B"),
    C(1, "C"),
    D(0, "D");

    private Integer code;
    private String name;

    SlaLevel(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static SlaLevel getByName(String name) {
        SlaLevel[] enums = SlaLevel.values();
        for (SlaLevel slaLevel : enums) {
            if (slaLevel.name.equals(name.toUpperCase())) {
                return slaLevel;
            }
        }
        log.error("parse SlaLevel by name exception, invalid name:[{}]", name);
        return null;
    }

    public static boolean compareSlaLevel(SlaLevel a, SlaLevel b) {
        return a.getCode() > b.getCode();
    }

}
