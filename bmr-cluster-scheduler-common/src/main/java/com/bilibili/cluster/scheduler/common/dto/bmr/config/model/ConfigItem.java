package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 配置item
 * @Date: 2024/6/7 14:56
 * @Author: nizhiqiang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigItem {
    private String configItemKey;
    private String configItemValue;

    public static List<ConfigItem> fillConfigItemInfoIntoConfigItem(List<ConfigItemInfo> configItemInfoList) {
        if (CollectionUtils.isEmpty(configItemInfoList)) {
            return Collections.emptyList();
        }
        return configItemInfoList.stream()
                .map(configItemInfo -> new ConfigItem(configItemInfo.getItemKey(), configItemInfo.getItemValue()))
                .collect(Collectors.toList());
    }
}