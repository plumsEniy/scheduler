package com.bilibili.cluster.scheduler.common.dto.metric.dto;

import com.bilibili.cluster.scheduler.common.enums.metric.MetricEnvEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MetricConfInfo {

    @ApiModelProperty("自增主键ID")
    private Long id;

    @ApiModelProperty("集群id")
    private Long clusterId;

    @ApiModelProperty("组件名")
    private String componentName;

    @ApiModelProperty("组件id")
    private Long componentId;

    @ApiModelProperty("监控组件别名")
    private String componentNameAlias;

    @ApiModelProperty("服务树ID")
    private String appId;

    @ApiModelProperty("监控集群别名")
    private String clusterAlias;

    @ApiModelProperty("监控对象类型")
    private String monitorObjectType;

    @ApiModelProperty("环境类型")
    private MetricEnvEnum envType;

    @ApiModelProperty("端口")
    private String ports;

    @ApiModelProperty("监控路径")
    private String path;

    @ApiModelProperty("创建时间")
    private LocalDateTime ctime;

    @ApiModelProperty("修改时间")
    private LocalDateTime mtime;

    @ApiModelProperty("是否删除")
    private Integer deleted;

}
