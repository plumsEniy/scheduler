package com.bilibili.cluster.scheduler.common.dto.hbo.model;

import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @Date: 2024/12/31 15:16
 * @Author: nizhiqiang
 */

@Data
public class UpdateHboJobParamsDto {
    Map<String, String> add;

    Map<String, String> remove;
}
