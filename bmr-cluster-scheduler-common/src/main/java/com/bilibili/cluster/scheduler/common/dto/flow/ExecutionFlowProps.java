package com.bilibili.cluster.scheduler.common.dto.flow;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployPackageType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowEffectiveModeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowReleaseScopeType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: 工作流参数
 * @Date: 2024/2/20 17:52
 * @Author: nizhiqiang
 */

@Data
public class ExecutionFlowProps {

    /**
     * 安装包版本id
     */
    private Long packageId;

    /**
     * 配置包版本id
     */
    private Long configId;

    /**
     * 组件名
     */
    private String componentName;

    /**
     * 集群名
     */
    private String clusterName;

    /**
     * 集群id
     */
    private Long clusterId;

    /**
     * 组件id
     */
    private Long componentId;

    /**
     * 角色名：yarn, clickhouse, hdfs... etc
     */
    private String roleName;

    /**
     * job执行方式
     */
    private String jobExecuteType;

    @ApiModelProperty(value = "变更类型：迭代、扩容、上下线")
    private FlowDeployType deployType;

    @ApiModelProperty(value = "发布类型：全量、灰度、yarn队列等")
    private FlowReleaseScopeType releaseScopeType;

    @ApiModelProperty(value = "包类型：安装包、配置包等")
    private FlowDeployPackageType deployPackageType;

    @ApiModelProperty(value = "是否重启标记位")
    private Boolean restart;

    @ApiModelProperty(value = "生效模式：重启｜立即 生效")
    private FlowEffectiveModeEnum effectiveMode;



}
