package com.bilibili.cluster.scheduler.common.dto.presto;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @description: catalog
 * @Date: 2024/6/6 16:56
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrestoCatalog {
    String fileName;
    List<ConfigItem> configItemList;
}
