package com.bilibili.cluster.scheduler.common.dto.presto;

import lombok.Data;

import java.util.List;

/**
 * @description: presto额外参数
 * @Date: 2024/6/12 19:13
 * @Author: nizhiqiang
 */

@Data
public class PrestoAdditionalProps {
    String key;
    List<String> valueList;
}
