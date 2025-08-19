package com.bilibili.cluster.scheduler.common.dto.bmr.flow;

import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowOpStrategy;
import com.bilibili.cluster.scheduler.common.enums.bmr.flow.BmrFlowStatus;
import com.bilibili.cluster.scheduler.common.enums.flow.FlowStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateBmrFlowDto {

    private Long flowId;

    private BmrFlowOpStrategy opStrategy;

    private BmrFlowStatus flowStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String applyState;
}
