package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms;

import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * prepare阶段填充的属性
 * isFilled代表是否填充过
 */
@Data
public class NNProxyDeployNodeExtParams extends NNProxyDeployNodePreFillExtParams {

    /**
     * 是否填充过属性,如果填充过则准备阶段无需再次执行
     */
    private boolean filled = false;

    /**
     * 安装包版本
     */
    private String packageVersion;

    /**
     * 配置包版本
     */
    private String configVersion;

    /**
     * 发布前包版本Id
     */
    private Long beforePackageId;

    /**
     * 发布前包版本
     */
    private String beforePackageVersion;

    /**
     * 发布前配置版本Id
     */
    private Long beforeConfigId;

    /**
     * 发布前配置版本
     */
    private String beforeConfigVersion;

    /**
     * 节点执行开始时间，用于指标校验
     */
    private LocalDateTime startTime;

    /**
     * dns域名
     */
    private String dnsHost;

    /**
     * 组件优先级
     */
    private int priority;


}
