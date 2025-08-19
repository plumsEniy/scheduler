package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.conf;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class NNProxyDeployPipelineConf {

    @Value("${nnproxy.deploy.projectId}")
    String projectCode;

    // 扩容
    @Value("${nnproxy.deploy.expansion}")
    String expansionPipelineId;

    // 迭代
    @Value("${nnproxy.deploy.iteration}")
    String iterationPipelineId;


}
