package com.bilibili.cluster.scheduler.common.dto.bmr.config.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

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
@Data
public class ConfigVersionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    private Long id;

    /**
     * 对应Yarn/ClickHouse/Kafka/HDFS
     */
    private String serviceName;

    /**
     * 集群ID
     */
    private Long clusterId;

    /**
     * 组件ID
     */
    private Long componentId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 版本号
     */
    private String versionNumber;

    /**
     * 版本描述
     */
    private String versionDesc;

    /**
     * 版本zip包的md5
     */
    private String versionMd5;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 修改人
     */
    private String updater;

    /**
     * boss路径
     */
    private String bossPath;

    /**
     * boss版本备份情况 默认等待上传
     */
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
    private Boolean stableVersionFlag = false;


}
