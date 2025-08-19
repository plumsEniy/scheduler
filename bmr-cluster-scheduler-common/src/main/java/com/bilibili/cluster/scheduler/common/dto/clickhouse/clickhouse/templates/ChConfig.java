package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2025/2/7 16:57
 * @Author: nizhiqiang
 */

@Data
public class ChConfig {

    /**
     * config_settings.xml
     */
    Map<String, String> settings;

    /**
     * zookeeper.yaml
     */
    ZookeeperDTO zookeeper;

    /**
     * profiles.xml
     */
    Map<String, String> profiles;

    /**
     * users.xml，其中default/networks/ip是ip需要特殊处理
     */
    Map<String, Object> users;

    /**
     * storage.xml文件
     */
    FileDTO files;

    List<ClickhouseCluster> clusters;

    @NoArgsConstructor
    @Data
    public static class FileDTO {
        @Alias("config.d/storage.xml")
        private String storage;
    }

    @NoArgsConstructor
    @Data
    public static class ZookeeperDTO {
        private List<ZookeeperResource> nodes;
    }
}
