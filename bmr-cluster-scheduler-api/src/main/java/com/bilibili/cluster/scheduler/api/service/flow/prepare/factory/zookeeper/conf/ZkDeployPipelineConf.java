package com.bilibili.cluster.scheduler.api.service.flow.prepare.factory.zookeeper.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @description: zk发布的配置
 * @Date: 2025/6/26 14:38
 * @Author: nizhiqiang
 */

@Data
@Configuration
public class ZkDeployPipelineConf {

    @Value("${zk.deploy.projectId}")
    String projectCode;

    // 扩容
    @Value("${zk.deploy.expansion}")
    String expansionPipelineId;

    // 下线
    @Value("${zk.deploy.eviction}")
    String evictionPipelineId;

    // 重启
    @Value("${zk.deploy.restart}")
    String restartPipelineId;

//    其他节点更新
    @Value("${zk.deploy.refreshConfig}")
    String refreshConfigPipelineId;

}
