package com.bilibili.cluster.scheduler.common.dto.node.req;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

/**
 * @description: 分页查询host
 * @Date: 2024/3/5 11:04
 * @Author: nizhiqiang
 */

@Data
public class QueryHostExecutionFlowPageReq {
    @NotBlank(message = "主机名不能为空")
    private String hostName;

    private String operator;

    private FlowStatusEnum flowStatus;

    @Positive(message = "pageNum不合法")
    private Integer pageNum = 1;
    @Positive(message = "pageSize不合法")
    private Integer pageSize = 10;
}
