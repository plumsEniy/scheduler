package com.bilibili.cluster.scheduler.common.dto.monitor.resp;

import com.bilibili.cluster.scheduler.common.dto.monitor.dto.MonitorInfo;
import com.bilibili.cluster.scheduler.common.response.BaseResp;
import lombok.Data;

/**
 * @description:
 * @Date: 2025/4/27 17:55
 * @Author: nizhiqiang
 */

@Data
public class QueryMonitorResp extends BaseResp {

    MonitorInfo data;
}
