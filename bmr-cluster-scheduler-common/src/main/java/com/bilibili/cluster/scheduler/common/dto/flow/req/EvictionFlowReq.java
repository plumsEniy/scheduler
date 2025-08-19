package com.bilibili.cluster.scheduler.common.dto.flow.req;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @description: 主机驱逐下线请求
 * @Date: 2024/3/5 15:59
 * @Author: nizhiqiang
 */
@Data
public class EvictionFlowReq {

    /**
     * 驱逐主机列表
     */
    @NotEmpty(message = "host list can not be empty")
    private List<String> excludeHostList;

    /**
     * 容错度
     */
    private Integer tolerance;

    /**
     * 工作流并发度
     */
    @Positive(message = "flow parallelism is illegal")
    private Integer flowParallelism;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * 是否自动重试
     */
    private Boolean autoRetry;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 发布原因
     */
    @NotEmpty(message = "变更原因不能为空")
    private String reason;
}
