package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile.FileServiceTemplate;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplatePodTemplate;
import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplateVolumeTemplate;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @Date: 2025/1/17 10:45
 * @Author: nizhiqiang
 */

@Data
public class ClickhouseJsonObj {
    List<TemplatePodTemplate> podTemplateList;

    PaasConfig paasConfig;

    FileServiceTemplate serviceTemplate;

    List<TemplateVolumeTemplate> volumeTemplateList;

    Map<String, String> settingMap;

    Map<String, String> profileMap;

    Map<String, Object> userMap;

    List<ZookeeperResource> zookeeperList;

    String storage;

    ShardsProps replicaShardsProps;

    List<Shards> replicaShardList;

    ShardsProps adminShardsPropList;

    List<Shards> adminShards;
}
