package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:  配置组与组的关联关系表
 * @Date: 2024/6/13 16:43
 * @Author: nizhiqiang
 */

@Data
public class ConfigGroupRelationEntity {

    /**
     * 自增主键ID
     */
    private Long id;

    /**
     * 组件版本表ID
     */
    private Long componentConfigVersionId;

    /**
     * 节点组id
     */
    private Long resourceGroupId;

    /**
     * 配置组名称
     */
    private String resourceGroupName;

    /**
     * 组描述
     */
    private String description;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 修改人
     */
    private String updater;

    /**
     * 数据状态 0:正常，1:删除
     */
    private Integer deleted =0;

    /**
     * 创建时间
     */
    private LocalDateTime ctime;

    /**
     * 修改时间
     */
    private LocalDateTime mtime;

}
