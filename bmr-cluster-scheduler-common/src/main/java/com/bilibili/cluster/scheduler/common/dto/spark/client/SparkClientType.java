package com.bilibili.cluster.scheduler.common.dto.spark.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Locale;

@AllArgsConstructor
public enum SparkClientType {

    SPARK("SPARK"),
    ONE_CLIENT("ONE-CLIENT"),
    ;
    @Getter
    String desc;

    public static SparkClientType getByComponentName(String componentName) {
        if (!StringUtils.hasText(componentName)) {
            return SPARK;
        }
        if (componentName.toUpperCase(Locale.ROOT).startsWith(SPARK.getDesc())) {
            return SPARK;
        }
        return ONE_CLIENT;
    }

}
