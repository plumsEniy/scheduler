package com.bilibili.cluster.scheduler.api.service.monitor;

import com.bilibili.cluster.scheduler.common.dto.monitor.dto.MonitorValue;

import java.util.List;

/**
 * @description: 监控指标
 * @Date: 2025/4/27 17:22
 * @Author: nizhiqiang
 */
public interface MonitorService {
    /**
     * 查询某个时间点的监控指标
     * @param promsql
     * @param timeStamp     不带毫秒的时间戳
     * @return
     */
    MonitorValue queryMonitor(String promsql, Long timeStamp);

    /**
     * 查询某个时间段内的监控指标
     * @param promsql   表达式
     * @param start     开始时间（必）
     * @param end       结束时间(必)
     * @param step      步长，单位秒
     * @return
     */
    List<MonitorValue> queryRangeMonitor(String promsql, Long start,Long end,Double step);

}
