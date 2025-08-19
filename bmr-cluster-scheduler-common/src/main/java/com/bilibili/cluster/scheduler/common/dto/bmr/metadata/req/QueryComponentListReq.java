package com.bilibili.cluster.scheduler.common.dto.bmr.metadata.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: 请求
 * @Date: 2024/5/14 16:34
 * @Author: nizhiqiang
 */

@Data
public class QueryComponentListReq {
    /**
     * 组件名
     */
    private String componentName;

    /**
     * 集群id
     */
    private Long clusterId;

    /**
     * 发布状态
     */
    private String releaseStatus;
}
