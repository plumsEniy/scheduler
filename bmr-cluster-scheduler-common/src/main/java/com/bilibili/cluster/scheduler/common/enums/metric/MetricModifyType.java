package com.bilibili.cluster.scheduler.common.enums.metric;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public enum MetricModifyType {

    CRON_SYNC_JOB("定时任务更新"),

    ADD_MONITOR_CONF("新增监控配置"),

    REMOVE_MONITOR_CONF("删除监控配置"),

    MODIFY_MONITOR_CONF("修改监控配置"),

    ;

    @Getter
    private String desc;

}
