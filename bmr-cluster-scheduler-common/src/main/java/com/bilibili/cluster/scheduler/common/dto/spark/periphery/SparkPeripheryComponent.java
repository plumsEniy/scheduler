package com.bilibili.cluster.scheduler.common.dto.spark.periphery;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SparkPeripheryComponent {

    ONE_CLIENT("one-client", "ONE-CLIENT"),

    ICEBERG("Iceberg", "ICEBERG"),

    CELEBORN("Celeborn", "CELEBORN"),

    RANGER("Ranger", "RANGER"),

    ;

    @Getter
    String desc;

    @Getter
    String alias;

}
