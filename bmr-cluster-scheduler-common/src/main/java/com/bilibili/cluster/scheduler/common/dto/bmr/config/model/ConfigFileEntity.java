package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.bilibili.cluster.scheduler.common.enums.bmr.config.ConfigVersionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 配置文件
 * @Date: 2024/6/13 17:11
 * @Author: nizhiqiang
 */

@Data
public class ConfigFileEntity {

    private Long id;

    /**
     * 组件版本表ID
     */
    private Long versionId;


    /**
     * 版本表类型NORMAL普通，SPECIAL 特殊类型
     */
    private ConfigVersionType versionType;


    /**
     * config_group_relation表ID
     */
    private Long configGroupRelationId;

    /**
     * 配置文件名
     */
    private String fileName;

    /**
     * 配置文件类型
     */
    private String fileType;

    /**
     * 文件内容
     */
    private String fileContent;

    /**
     * 文件的md5
     */
    private String fileMd5;

    /**
     * 文件是否解析,0:false,1:true
     */
    private Boolean analysis;

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
    private Boolean deleted =false;

    /**
     * 创建时间
     */
    @TableField("ctime")
    private LocalDateTime ctime;

    /**
     * 修改时间
     */
    @TableField("mtime")
    private LocalDateTime mtime;
}
