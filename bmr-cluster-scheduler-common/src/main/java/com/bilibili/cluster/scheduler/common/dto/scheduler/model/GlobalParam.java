package com.bilibili.cluster.scheduler.common.dto.scheduler.model;

import lombok.Data;

/**
 * @description: 全局参数
 * @Date: 2024/5/13 18:05
 * @Author: nizhiqiang
 */

@Data
public class GlobalParam {

    private String prop;
    private String direct;
    private String type;
    private String value;
}
