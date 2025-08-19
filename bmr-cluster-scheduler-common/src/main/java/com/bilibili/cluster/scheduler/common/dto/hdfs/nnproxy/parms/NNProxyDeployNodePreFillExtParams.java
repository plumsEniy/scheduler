package com.bilibili.cluster.scheduler.common.dto.hdfs.nnproxy.parms;

import com.bilibili.cluster.scheduler.common.dto.params.BaseNodeParams;
import lombok.Data;

/**
 * generator node阶段填充的属性
 */
@Data
public class NNProxyDeployNodePreFillExtParams extends BaseNodeParams {

    /**
     * 是否包含安装包发布
     */
    private boolean isContainPackage;

    /**
     * 四否包含配置发布
     */
    private boolean isContainConfig;

    /**
     * 安装包id
     */
    private Long packageId;

    /**
     * 组件Id
     */
    private Long componentId;
    /**
     * 配置包id
     */
    private Long configId;


}
