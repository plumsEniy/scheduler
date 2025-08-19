package com.bilibili.cluster.scheduler.common.dto.hadoop;

import lombok.Data;

import java.util.List;

/**
 * @description: fastdecomission的详情
 * @Date: 2024/5/13 10:44
 * @Author: nizhiqiang
 */
@Data
public class FastDecommissionTaskDTO {
    private FastDecommissionTask task;
    private List<String> logUrls;
}
