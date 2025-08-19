package com.bilibili.cluster.scheduler.common.dto.bmr.config;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 组件配置版本表
 * </p>
 *
 * @author szh
 * @since 2023年11月09日
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("component_config_version_service")
public class ComponentConfigVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对应Yarn/ClickHouse/Kafka/HDFS
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 集群ID
     */
    @TableField("cluster_id")
    private Long clusterId;

    /**
     * 组件ID
     */
    @TableField("component_id")
    private Long componentId;

    /**
     * 组件名称
     */
    @TableField("component_name")
    private String componentName;

    /**
     * 版本号
     */
    @TableField("version_number")
    private String versionNumber;

    /**
     * 版本描述
     */
    @TableField("version_desc")
    private String versionDesc;

    /**
     * 版本zip包的md5
     */
    @TableField("version_md5")
    private String versionMd5;

    /**
     * 创建人
     */
    @TableField("creator")
    private String creator;

    /**
     * 修改人
     */
    @TableField("updater")
    private String updater;

    /**
     * boss路径
     */
    @TableField("boss_path")
    private String bossPath;

    /**
     * boss版本备份情况 默认等待上传
     */
    @TableField("boss_status")
    private String bossStatus;

    /**
     * 数据状态 0:正常，1:删除
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted = false;


    /**
     * 创建时间
     */
    @TableField("ctime")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ctime;

    /**
     * 修改时间
     */
    @TableField("mtime")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mtime;

    /**
     * 是否为稳定版本标记,0:false,1:true
     */
    @TableField("stable_version_flag")
    private Boolean stableVersionFlag = false;


}
