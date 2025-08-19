package com.bilibili.cluster.scheduler.common.dto.monitor.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2025/4/27 17:58
 * @Author: nizhiqiang
 */

@Data
public class MonitorResult {

    /**
     * 目前用不到
     */
    Map<String, String> metric;

    /**
     * 查询单个时间点监控的时候的结果
     * [1745746256,"0"]
     */
    List<Object> value;

    /**
     * 查询时间段监控的时候的结果
     */
    List<List<Object>> values;
}
