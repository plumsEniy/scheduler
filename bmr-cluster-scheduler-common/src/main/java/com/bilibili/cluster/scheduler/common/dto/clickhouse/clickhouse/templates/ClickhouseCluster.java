package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates;

import com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.template.TemplateSchemaPolicy;
import com.bilibili.cluster.scheduler.common.enums.clickhouse.CKClusterType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description:
 * @Date: 2025/2/7 17:06
 * @Author: nizhiqiang
 */

@Data
public class ClickhouseCluster {

    private SecretDTO secret;

    private LayoutDTO layout;

    private TemplateSchemaPolicy schemaPolicy;

    private String name;

//    只有业务含义json中不存在
    private transient CKClusterType clusterType;

    @NoArgsConstructor
    @Data
    public static class SecretDTO {
        private String value;
    }

    @NoArgsConstructor
    @Data
    public static class LayoutDTO {
        private List<Shards> shards;
    }

    public CKClusterType getClusterType() {
        return clusterType;
    }
}
