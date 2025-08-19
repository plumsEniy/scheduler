package com.bilibili.cluster.scheduler.common.dto.flow;

import com.bilibili.cluster.scheduler.common.enums.flow.FlowRollbackType;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateExecutionFlowDTO {

    private Long flowId;

    private FlowStatusEnum flowStatus;

    private String hostName;

    private Integer currentBatchId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private FlowRollbackType rollbackType;
}
