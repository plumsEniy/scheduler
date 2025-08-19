package com.bilibili.cluster.scheduler.common.dto.metric.resp;

import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricNodeInstance;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询采集任务列表
 * @Date: 2024/8/30 15:09
 * @Author: nizhiqiang
 */

@Data
public class QueryMetricInstanceListResp extends BaseResp {
    List<MetricNodeInstance> data;
}
