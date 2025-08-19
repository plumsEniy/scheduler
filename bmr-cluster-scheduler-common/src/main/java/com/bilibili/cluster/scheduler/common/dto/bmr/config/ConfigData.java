package com.bilibili.cluster.scheduler.common.dto.bmr.config;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItemInfo;
import lombok.Data;

import java.util.List;

/**
 * @description: 配置数据
 * @Date: 2024/6/7 15:07
 * @Author: nizhiqiang
 */
@Data
public class ConfigData {
    private List<ConfigItemInfo> items;

    private String context;
}
