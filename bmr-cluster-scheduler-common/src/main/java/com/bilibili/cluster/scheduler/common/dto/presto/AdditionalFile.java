package com.bilibili.cluster.scheduler.common.dto.presto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @description: 额外文件
 * @Date: 2024/6/6 16:56
 * @Author: nizhiqiang
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalFile {
    String fileName;
    Map<String, String> map;
}
