package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import lombok.Data;

/**
 * @description: 配置组
 * @Date: 2024/5/15 16:20
 * @Author: nizhiqiang
 */
@Data
public class ConfigGroupDo {

    private int logicGroupId;

    private String dirName;

    private boolean defaultGroup;
}
