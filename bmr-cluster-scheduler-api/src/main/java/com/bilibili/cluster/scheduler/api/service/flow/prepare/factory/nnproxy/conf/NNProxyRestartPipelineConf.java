package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.nnproxy.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class NNProxyRestartPipelineConf {

    @Value("${nnproxy.deploy.projectId}")
    String projectCode;

    // 重启
    @Value("${nnproxy.deploy.restart}")
    String restartPipelineId;

    // 停止
    @Value("${nnproxy.deploy.stop}")
    String stopPipelineId;

}
