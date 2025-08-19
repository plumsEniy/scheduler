package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import lombok.Data;

/**
 * @description: 安装包
 * @Date: 2024/5/15 14:57
 * @Author: nizhiqiang
 */

@Data
public class MetadataPackageData {

    private int id;
    private String tagName;
    private String message;
    private String deliveryStatus;
    private String releaseStatus;
    private String ciBranch;
    private String commitId;
    private String storagePath;
    private String productBagName;
    private String productBagMd5;
    private String founder;
    private String ctime;
    private String mtime;
    private boolean deleted;
    private int componentId;
    private String imagePath;
    // for spark ci pack info
    private String componentName;
    private String packageType;
    private String seniorVersion;
    private String minorVersion;
    private boolean isTest;
    private boolean fallback;

    private String upperService;


}
