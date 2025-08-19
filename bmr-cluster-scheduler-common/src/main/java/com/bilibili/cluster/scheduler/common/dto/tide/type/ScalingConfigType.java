package com.bilibili.cluster.scheduler.common.dto.tide.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ScalingConfigType {

    PRESTO("presto"),
    CLICKHOUSE("clickhouse"),
    ;

    private String desc;

}
