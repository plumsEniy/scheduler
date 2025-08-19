package com.bilibili.cluster.scheduler.common.dto.hadoop;

import lombok.Data;

import java.util.List;

/**
 * @description: fast decomission详情
 * @Date: 2024/5/13 10:42
 * @Author: nizhiqiang
 */

@Data
public class FastDecommissionTask {
    List<String> datanodes;
    String status;
    String hadoopConfDir;
    String mtime;
    String ctime;
    String workerIP;
    Long datanodesConfigId;
    Long id;
}
