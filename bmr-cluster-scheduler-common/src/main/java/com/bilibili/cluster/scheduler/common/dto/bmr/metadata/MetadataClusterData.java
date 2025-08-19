package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import com.bilibili.cluster.scheduler.common.dto.bmr.metadata.enums.ClusterNetworkEnvironmentEnum;
import com.bilibili.cluster.scheduler.common.enums.bmr.metadata.ClusterTypeEnum;
import lombok.Data;

/**
 * @description: 集群信息
 * @Date: 2024/5/23 09:02
 * @Author: nizhiqiang
 */
@Data
public class MetadataClusterData {
    private int id;
    private String clusterName;
    private String businessType;
    private String clusterEnvironment;
    private ClusterTypeEnum clusterType;
    private String principal;
    private boolean deleted;
    private String ctime;
    private String mtime;
    private String upperService;
    private String owner;
    private String jobExecuteType;
    private ClusterNetworkEnvironmentEnum networkEnvironment;
    private String appId;
    private String anotherName;
    private Long runningPackageId;
    private Long runningConfigId;
}
