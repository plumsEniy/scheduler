package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @Date: 2025/4/28 17:09
 * @Author: nizhiqiang
 */
@Data
public class BasePage<T> {
    List<T> list;
}
