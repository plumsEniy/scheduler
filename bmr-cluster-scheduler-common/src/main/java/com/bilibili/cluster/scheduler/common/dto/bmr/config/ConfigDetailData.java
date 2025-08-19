package com.bilibili.cluster.scheduler.common.dto.bmr.config;

import lombok.Data;

/**
 * @description: 配置详情
 * @Date: 2024/5/15 15:39
 * @Author: nizhiqiang
 */

@Data
public class ConfigDetailData {

    private Long id;

    private String configVersionNumber;

    private String configVersionMd5;

    private String downloadUrl;

    private Long componentId;
}
