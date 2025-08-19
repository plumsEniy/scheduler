package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NNProxyStage {

    STAGE_1(1),
    STAGE_2(2),
    STAGE_3(3),
    STAGE_4(4),
    STAGE_5(5),
    STAGE_6(6),
    ;

    @Getter
    int stage;

}
