package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.presto.conf;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class PrestoTidePipelineConf {

    @Value("${presto.tide.projectId}")
    String projectCode;

    // 扩容
    @Value("${presto.tide.yarn.node.expansion}")
    String yarnExpansionPipelineId;

    // 缩容
    @Value("${presto.tide.yarn.node.shrink}")
    String yarnEvictionPipelineId;

}
