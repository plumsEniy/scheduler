package com.bilibili.cluster.scheduler.api.event.spark.client.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SparkClientPackDeployPipelineInfo {

    @Value("${spark.client.pack.operate.projectId}")
    String projectCode;

    @Value("${spark.client.pack.pipelineId.type.clean}")
    String cleanPipelineId;

    @Value("${spark.client.pack.pipelineId.type.download}")
    String downloadPipelineId;

    @Value("${spark.client.pack.pipelineId.type.remove}")
    String removePipelineId;

}
