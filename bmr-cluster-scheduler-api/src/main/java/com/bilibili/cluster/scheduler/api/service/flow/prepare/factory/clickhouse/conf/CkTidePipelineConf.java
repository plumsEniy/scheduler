package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.clickhouse.conf;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class CkTidePipelineConf {

    @Value("${ck.tide.projectId}")
    String projectCode;

    // 扩容
    @Value("${ck.tide.yarn.node.expansion}")
    String yarnDeployPipelineId;

    // 缩容
    @Value("${ck.tide.yarn.node.shrink}")
    String yarnEvictionPipelineId;

}
