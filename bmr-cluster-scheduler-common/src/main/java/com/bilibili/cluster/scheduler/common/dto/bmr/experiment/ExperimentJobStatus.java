package com.bilibili.cluster.scheduler.common.dto.bmr.experiment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExperimentJobStatus {

   //  NOT_RUN("未运行"),
    RUNNING("运行中"),
    SUCCESS("运行成功"),
    WAITING_COST("等待cost指标"),
    FAIL("运行失败");

    private String desc;

}
