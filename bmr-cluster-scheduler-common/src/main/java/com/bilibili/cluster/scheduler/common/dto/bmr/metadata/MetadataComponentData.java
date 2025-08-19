package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import lombok.Data;

/**
 * @description: 组件信息
 * @Date: 2024/5/14 16:31
 * @Author: nizhiqiang
 */
@Data
public class MetadataComponentData {

    private int id;
    private String componentName;
    private int clusterId;
    private String releaseStatus;
    private String releaseType;
    private Integer nodeWarningsNumber;
    private String ctime;
    private String mtime;
    private boolean deleted;

    private int priority;

}
