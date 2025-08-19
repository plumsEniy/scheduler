package com.bilibili.cluster.scheduler.common.dto.spark.manager;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @description: 任务Label
 * @Date: 2025/3/5 18:57
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum SparkJobLabel  {
    DEFAULT("默认", 99999),
    STAGE_1("第一阶段", 1),
    STAGE_2("第二阶段", 2),
    STAGE_3("第三阶段", 3),
    STAGE_4("第四阶段", 4),
    STAGE_5("第五阶段", 5),
    STAGE_6("第六阶段", 6),
    STAGE_7("第七阶段", 7),
    ;

    private String desc;

    @Getter
    private Integer order;

    @Getter
    private static final List<SparkJobLabel> stageList = Arrays.asList(STAGE_1, STAGE_2, STAGE_3, STAGE_4, STAGE_5, STAGE_6, STAGE_7);

}
