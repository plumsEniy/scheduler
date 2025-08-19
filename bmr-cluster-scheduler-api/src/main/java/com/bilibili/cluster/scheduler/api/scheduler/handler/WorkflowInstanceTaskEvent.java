package com.bilibili.cluster.scheduler.api.scheduler.handler;

import com.bilibili.cluster.scheduler.common.dto.flow.ExecutionFlowInstanceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class WorkflowInstanceTaskEvent {

    private Long instanceId;

    private Integer batchId;

    private Long flowId;

    private String hostName;

    private ExecutionFlowInstanceDTO executionFlowInstanceDTO;

}
