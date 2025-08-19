package com.bilibili.cluster.scheduler.common.dto.bmr.config;

import com.bilibili.cluster.scheduler.common.dto.bmr.config.model.ConfigGroupDo;
import lombok.Data;

import java.util.List;

/**
 * @description: 查询group
 * @Date: 2024/5/15 16:23
 * @Author: nizhiqiang
 */

@Data
public class ConfigGroupDto {
    private List<ConfigGroupDo> logicGroups;

}
