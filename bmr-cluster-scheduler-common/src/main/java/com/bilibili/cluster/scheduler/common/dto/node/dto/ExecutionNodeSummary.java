package com.bilibili.cluster.scheduler.common.dto.node.dto;

import com.bilibili.cluster.scheduler.common.enums.NodeExecuteStatusEnum;
import lombok.Data;

@Data
public class ExecutionNodeSummary {

    private String execStage;

    private NodeExecuteStatusEnum executeStatus;

    private long count;

}
