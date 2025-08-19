package com.bilibili.cluster.scheduler.common.dto.bmr.metadata;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Data
public class InstallationPackage {

    private Long id;
    /**
     * 安装包名/tag
     */
    private String tagName;
    /**
     * 打tag传回的描述
     */
    private String message;
    /**
     * 交付状态
     */
    private String deliveryStatus;
    /**
     * 发布状态
     */
    private String releaseStatus;
    /**
     * ci分支
     */
    private String ciBranch;
    /**
     * commit id
     */
    private String commitId;
    /**
     * boss存储路径
     */
    private String storagePath;
    /**
     * 产物包名称
     */
    private String productBagName;
    /**
     * 产物包md5
     */
    private String productBagMd5;
    /**
     * 创建人
     */
    private String founder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime ctime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime mtime;

    @TableLogic
    @TableField(select = false)
    private Boolean deleted;


    /**
     * 组件表id
     */
    private Long componentId;

    /**
     * 组件名
     */
    private String componentName;

    private Integer stableVersion;

    /**
     * nyx构建
     */
    private Long buildTaskId;

    /**
     * 镜像的路径
     */
    private String imagePath;

    /**
     * 安装包类型：组件还是component
     */
    private String packageType;

    /**
     * 大版本
     */
    private String seniorVersion;

    /**
     * 小版本
     */
    private String minorVersion;

    /**
     * 是否为测试
     */
    private boolean isTest;
}
