package com.bilibili.cluster.scheduler.common.dto.bmr.experiment;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExperimentType {

    COMPARATIVE_TASK("A->B对比实验"),

    PERFORMANCE_TEST("性能验证实验"),
    ;

    String desc;

}
