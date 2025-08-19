package com.bilibili.cluster.scheduler.common.dto.monitor.dto;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2025/4/27 17:56
 * @Author: nizhiqiang
 */

@lombok.NoArgsConstructor
@Data
public class MonitorInfo {

    /**
     * 返回指标类型，vector或者matrix
     */
    private String resultType;

    private List<MonitorResult> result;

}
