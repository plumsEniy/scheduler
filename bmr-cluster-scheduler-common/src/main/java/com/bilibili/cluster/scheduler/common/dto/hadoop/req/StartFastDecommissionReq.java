package com.bilibili.cluster.scheduler.common.dto.hadoop.req;

import lombok.Data;

import java.util.List;

/**
 * @description: 启动decomission
 * @Date: 2024/5/13 11:04
 * @Author: nizhiqiang
 */
@Data
public class StartFastDecommissionReq {

    private List<String> nodes;
    private Long dnId;
    private List<String> hadoopConfDirs;
    private String dc;
    private String clusterVersion;
}
