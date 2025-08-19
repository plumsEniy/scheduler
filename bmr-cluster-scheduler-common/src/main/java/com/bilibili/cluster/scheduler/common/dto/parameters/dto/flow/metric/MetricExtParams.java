package com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.metric;

import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import com.bilibili.cluster.scheduler.common.enums.metric.MetricModifyType;
import lombok.Data;

import java.util.List;


@Data
public class MetricExtParams {

    private MetricModifyType modifyType;

    private String componentAliasName;

    private MetricEnvEnum envType;

    private MetricConfInfo before;

    private List<MetricConfInfo> afterList;

    private Integer integrationId;

    private String token;

}
