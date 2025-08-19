package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import lombok.Data;

@Data
public class MetadataMonitorConf {

    private String component;

    private Integer integrationId;

    private String token;

    private MetricConfInfo monitorComponent;

}
