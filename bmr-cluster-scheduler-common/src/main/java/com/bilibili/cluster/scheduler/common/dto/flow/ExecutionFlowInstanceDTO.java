package com.bilibili.cluster.scheduler.common.dto.flow;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import java.io.Serializable;


@Data
public class ExecutionFlowInstanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /***
     *  唯一标识
     * */
    private Long instanceId;

    /***
     * flowId
     * */
    private Long flowId;

    /***
     * batchId 批次ID 当前执行批次号
     * */
    private Integer currentBatchId;

    /***
     *  最大执行批次号
     * */
    private Integer maxBatchId;

    /***
     * 工作流类型
     * */
    private FlowDeployType deployType;

    /***
     * 执行实例主机
     * */
    private String hostName;

    /**
     * flow状态
     */
    private FlowStatusEnum flowStatusEnum;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 是否自动重试
     */
    private boolean autoRetry = true;

    /**
     * 最大重试次数
     */
    private Integer maxRetry = 1;

    /**
     * 工作流参数
     */
    private ExecutionFlowProps executionFlowProps;

}
