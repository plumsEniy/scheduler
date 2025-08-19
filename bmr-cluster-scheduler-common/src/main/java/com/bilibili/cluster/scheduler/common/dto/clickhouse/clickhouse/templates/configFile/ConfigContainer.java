package com.bilibili.cluster.scheduler.common.dto.clickhouse.clickhouse.templates.configFile;

import lombok.Data;

/**
 * @description:
 * @Date: 2025/1/21 11:25
 * @Author: nizhiqiang
 */
@Data
public class ConfigContainer {
    String name;

    String image;

    private ContainerResource resources;

    @Data
    public static class ContainerResource {
        ConfigContainerResource requests;

        ConfigContainerResource limits;
    }

}
