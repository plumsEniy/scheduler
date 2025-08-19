package com.bilibili.cluster.scheduler.common.dto.bmr.experiment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ExperimentJobType {

    COMPASS_JOB("线上用户任务", "compass"),

    TEST_JOB("手动测试任务", "manual"),
    ;

    @Getter
    String desc;

    @Getter
    String type;

}
