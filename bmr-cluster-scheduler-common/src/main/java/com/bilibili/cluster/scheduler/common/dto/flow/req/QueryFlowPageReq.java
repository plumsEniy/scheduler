package com.bilibili.cluster.scheduler.common.dto.flow.req;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowDeployType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowEffectiveModeEnum;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * @description: 查询工作流的请求
 * @Date: 2024/1/30 19:22
 * @Author: nizhiqiang
 */

@Data
public class QueryFlowPageReq {
    private FlowDeployType deployType;

    private FlowStatusEnum flowStatus;

    private  long flowId;

    private String operator;

    private String nodeName;

    private String componentName;

    private FlowEffectiveModeEnum effectiveMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Positive(message = "page num is illegal")
    private int pageNum = 1;

    @Positive(message = "page size is illegal")
    private int pageSize = 10;
}
