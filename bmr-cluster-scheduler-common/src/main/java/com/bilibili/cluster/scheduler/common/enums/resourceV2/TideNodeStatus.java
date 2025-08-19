package com.bilibili.cluster.scheduler.common.enums.resourceV2;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TideNodeStatus {

    STAIN("STAIN"), // 污点
    AVAILABLE("AVAILABLE"), // 可用
    UN_AVAILABLE("UN_AVAILABLE"), // 不可用
    ;

    @Getter
    final String value;

}
