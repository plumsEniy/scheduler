package com.bilibili.cluster.scheduler.common.enums.scheduler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @description: dolp的失败策略
 * @Date: 2024/3/26 16:17
 * @Author: nizhiqiang
 */

@AllArgsConstructor
public enum DolpFailureStrategy {
    END("失败结束"),
    CONTINUE("失败后继续"),
    ;


    @Getter
    private String desc;

    public static DolpFailureStrategy getByValue(String failureStrategy) {
        if (StringUtils.isBlank(failureStrategy)) {
            return CONTINUE;
        }
        if (END.name().equalsIgnoreCase(failureStrategy)) {
            return END;
        } else {
            return CONTINUE;
        }
    }
}
