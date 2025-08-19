package com.bilibili.cluster.scheduler.common.dto.spark.params;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public enum SparkDeployType {

    NORMAL("普通发布"),

    EMERGENCY("紧急发布"),

    ;
    @Getter
    String desc;

}
