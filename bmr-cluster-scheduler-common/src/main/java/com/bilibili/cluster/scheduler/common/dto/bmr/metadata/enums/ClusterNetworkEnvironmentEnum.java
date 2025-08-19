package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClusterNetworkEnvironmentEnum {

    UAT("uat"),
    PRE("pre"),
    PROD("prod"),
    ;

    @Getter
    private String env;
}
