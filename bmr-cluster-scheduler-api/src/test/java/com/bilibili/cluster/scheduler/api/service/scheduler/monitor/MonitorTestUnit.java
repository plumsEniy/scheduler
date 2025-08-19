package com.bilibili.cluster.scheduler.api.service.scheduler.monitor;

import cn.hutool.json.JSONUtil;
import com.bilibili.cluster.scheduler.common.dto.flow.prop.BaseFlowExtPropDTO;
import com.bilibili.cluster.scheduler.common.dto.metric.dto.MetricConfInfo;
import com.bilibili.cluster.scheduler.common.dto.parameters.dto.flow.metric.MetricExtParams;
import org.junit.Test;

import java.util.List;

public class MonitorTestUnit {


    @Test
    public void testMonitExtPrams() {
        String extParamsJson = "{\"flowExtParams\":\"{    \\\"modifyType\\\": \\\"CRON_SYNC_JOB\\\",    \\\"before\\\": {        \\\"componentId\\\": 5,        \\\"ports\\\": \\\"9250\\\",        \\\"clusterAlias\\\": \\\"Amiya\\\",        \\\"componentName\\\": \\\"Amiya\\\"    },    \\\"afterList\\\": [        {            \\\"componentId\\\": 5,            \\\"clusterId\\\": 2,            \\\"monitorObjectType\\\": \\\"servers\\\",            \\\"ports\\\": \\\"9250,12345\\\",            \\\"componentNameAlias\\\": \\\"Amiya\\\",            \\\"path\\\": \\\"NaN\\\",            \\\"envType\\\": \\\"PROD\\\",            \\\"appId\\\": \\\"datacenter.hadoop.jscs-bigdata-offline-amiya-moni\\\",            \\\"componentName\\\": \\\"Amiya\\\",            \\\"clusterAlias\\\": \\\"jscs-bigdata-offline-amiya-moni\\\"        }    ],    \\\"integrationId\\\": 26,\\\"token\\\": \\\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMTgyNjI3NjcsImlhdCI6MTcwMjkwMjc2NywiaXNzIjoibml6aGlxaWFuZyIsIlBsYXRmb3JtTmFtZSI6ImJtci3mjIfmoIfnm5HmjqciLCJFeHBpcmVBdCI6IjAwMDEtMDEtMDFUMDA6MDA6MDBaIn0.Sw7qDeZJp2iyMZeuSEZ5gZtMiv5XDfnCXOSmtuBd3tI\\\",    \\\"componentAliasName\\\": \\\"Flink-k8s\\\",    \\\"envType\\\": \\\"PROD\\\"}\",\"nodeList\":[]}";

        BaseFlowExtPropDTO baseFlowExtPropDTO = JSONUtil.toBean(extParamsJson, BaseFlowExtPropDTO.class);

        System.out.println(baseFlowExtPropDTO);

        String flowExtParams = baseFlowExtPropDTO.getFlowExtParams();

        MetricExtParams metricExtParams = JSONUtil.toBean(flowExtParams, MetricExtParams.class);
        Integer integrationId = metricExtParams.getIntegrationId();
        List<MetricConfInfo> afterMonitorList = metricExtParams.getAfterList();

        System.out.println(afterMonitorList);








    }






}
