package com.bilibili.cluster.scheduler.common.dto.bmr.resourceV2.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
    * 节点组表
    */
@Data
public class ResourceLogicGroup {
    /**
    * 自增主键ID
    */
    private Long id;

    /**
    * 集群id
    */
    private Long clusterId;

    /**
    * 分组名称
    */
    private String groupName;

    /**
    * 节点组描述
    */
    private String groupDescribe;

    /**
    * 创建者
    */
    private String creator;

    /**
    * 更新者
    */
    private String updater;

    /**
    * 创建时间
    */
    private LocalDateTime ctime;

    /**
    * 修改时间
    */
    private LocalDateTime mtime;

    /**
    * 是否删除
    */
    private Integer deleted;

    private Integer hostCount;
}